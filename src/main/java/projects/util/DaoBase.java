/**
 * 
 */
package projects.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * This class contains utility methods for the DAO class.
 * 
 * @author Promineo
 *
 */
public abstract class DaoBase {
  /**
   * This starts a MySQL transaction. Normally MySQL starts a transaction before every request and
   * commits it after each request. By turning auto-commit off, the transaction is only committed
   * when the application tells it to.
   * 
   * @param conn The connection on which to start the transaction.
   * @throws SQLException Thrown if an error occurs starting the transaction.
   */
  protected void startTransaction(Connection conn) throws SQLException {
    conn.setAutoCommit(false);
  }

  /**
   * Commit the transaction. This will write all the changes, if any, to the database.
   * 
   * @param conn The connection on which to commit the transaction.
   * @throws SQLException Thrown if an error occurs committing the transaction.
   */
  protected void commitTransaction(Connection conn) throws SQLException {
    conn.commit();
  }

  /**
   * Rolls back the changes so that nothing is committed.
   * 
   * @param conn The connection on which to roll back the transaction.
   * @throws SQLException Thrown if an error occurs rolling back the transaction.
   */
  protected void rollbackTransaction(Connection conn) throws SQLException {
    conn.rollback();
  }

  /**
   * This sets a parameter on a prepared statement. If the parameter is null, it is handled
   * correctly.
   * 
   * @param stmt The prepared statement on which to set the parameter.
   * @param parameterIndex This is the one-based index of the parameter. In the SQL that is bound to
   *        the prepared statement, parameters are indicated by a question mark. From left-to-right,
   *        the index is the order in which a question mark is encountered.
   * @param value The parameter value. This may be null.
   * @param classType This is the Java class type of the parameter. It is used to select the correct
   *        method on the driver so that the parameter is added correctly. It is also used to set
   *        the type in case the parameter is null.
   * @throws SQLException Thrown if an error occurs.
   */
  protected void setParameter(PreparedStatement stmt, int parameterIndex, Object value,
      Class<?> classType) throws SQLException {
    int sqlType = convertJavaClassToSqlType(classType);

    if(Objects.isNull(value)) {
      stmt.setNull(parameterIndex, sqlType);
    }
    else {
      switch(sqlType) {
        case Types.DECIMAL:
          stmt.setBigDecimal(parameterIndex, (BigDecimal)value);
          break;

        case Types.DOUBLE:
          stmt.setDouble(parameterIndex, (Double)value);
          break;

        case Types.INTEGER:
          stmt.setInt(parameterIndex, (Integer)value);
          break;

        case Types.OTHER:
          stmt.setObject(parameterIndex, value);
          break;

        case Types.VARCHAR:
          stmt.setString(parameterIndex, (String)value);
          break;

        default:
          throw new DaoException("Unknown parameter type: " + classType);
      }
    }
  }

  /**
   * Converts from a Java class to a java.sql.Types value.
   * 
   * @param classType The class type
   * @return A java.sql.Types value
   */
  private int convertJavaClassToSqlType(Class<?> classType) {
    if(Integer.class.equals(classType)) {
      return Types.INTEGER;
    }

    if(String.class.equals(classType)) {
      return Types.VARCHAR;
    }

    if(Double.class.equals(classType)) {
      return Types.DOUBLE;
    }

    if(BigDecimal.class.equals(classType)) {
      return Types.DECIMAL;
    }

    if(LocalTime.class.equals(classType)) {
      return Types.OTHER;
    }

    throw new DaoException("Unsupported class type: " + classType.getName());
  }

  /**
   * This retrieves the number of child rows and adds one to the value. It is used to set the order
   * of a child row. For a *real* application, a more sophisticated approach is desired. This method
   * does not allow for entity reordering and does not allow for an entity to be deleted.
   * 
   * @param conn The connection
   * @param id The ID of the parent entity
   * @param tableName The name of the table with the child rows
   * @param idName The name of the parent ID field
   * @return The count of the entities attached to the parent plus one
   * @throws SQLException Thrown if an error occurs.
   */
  protected Integer getNextSequenceNumber(Connection conn, Integer id, String tableName,
      String idName) throws SQLException {
    String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + idName + " = ?";

    try(PreparedStatement stmt = conn.prepareStatement(sql)) {
      setParameter(stmt, 1, id, Integer.class);

      try(ResultSet rs = stmt.executeQuery()) {
        if(rs.next()) {
          return rs.getInt(1) + 1;
        }

        return 1;
      }
    }
  }

  /**
   * This returns the integer primary key value of the last row inserted into the given table. It
   * allows the ID to be inserted into the entity object after inserting it into the table.
   * 
   * The other way of doing this is to call {@link Statement#getGeneratedKeys()}, but this returns a
   * result set that needs to be parsed for the ID. It's not really any easier than this approach
   * but should be closer to database-agnostic.
   * 
   * @param conn The connection
   * @param table The name of the table on which to get the last inserted primary key value
   * @return The primary key value
   * @throws SQLException Thrown if an error occurs
   */
  protected Integer getLastInsertId(Connection conn, String table) throws SQLException {
    String sql = String.format("SELECT LAST_INSERT_ID() FROM %s", table);

    try(Statement stmt = conn.createStatement()) {
      try(ResultSet rs = stmt.executeQuery(sql)) {
        if(rs.next()) {
          return rs.getInt(1);
        }

        throw new SQLException("Unable to retrieve the primary key value. No result set!");
      }
    }
  }

  /**
   * This extracts an object of the given type from a result set. The object must have a
   * zero-argument constructor. It builds an object from a result set using reflection as follows:
   * <ol>
   * <li>The zero-argument constructor is obtained.</li>
   * <li>An object of the given class type is created.</li>
   * <li>A list of fields is obtained using reflection.</li>
   * <li>The field name is converted from Java naming to SQL naming conventions (camel case to snake
   * case). Obviously, for this to work, the Java name must match the column name. So, if the Java
   * name is numServings, the column name must be num_servings.</li>
   * <li>The value is assigned to the field in the object.</li>
   * </ol>
   * 
   * Example: if a query returns values for a recipe, a Recipe object is returned. So:
   * 
   * <pre>
   * String sql = "SELECT * from recipe";
   * ResultSet rs = getResultSetSomehow();
   * 
   * Recipe recipe = extract(rs, Recipe.class);
   * </pre>
   * 
   * Note: if the Java field does not exist in the result set, the value of the field is left
   * unchanged. So, class Recipe has an instance variable:
   * 
   * <pre>
   * List<Ingredient> ingredients = new LinkedList<>();
   * </pre>
   * 
   * Since the result set does not contain a column named "ingredients", the value is left alone and
   * the list initialization is preserved.
   * 
   * @param <T> The Generic for the type of object to create and return.
   * @param rs The result set in which to extract values. The result set must be positioned on the
   *        correct row by the caller.
   * @param classType The actual class type of the object to create.
   * @return A populated class.
   */
  protected <T> T extract(ResultSet rs, Class<T> classType) {
    try {
      /* Obtain the constructor and create an object of the correct type. */
      Constructor<T> con = classType.getConstructor();
      T obj = con.newInstance();

      /* Get the list of fields and loop through them. */
      for(Field field : classType.getDeclaredFields()) {
        String colName = camelCaseToSnakeCase(field.getName());
        Class<?> fieldType = field.getType();

        /*
         * Set the field accessible flag which means that we can populate even private fields
         * without using the setter.
         */
        field.setAccessible(true);
        Object fieldValue = null;

        try {
          fieldValue = rs.getObject(colName);
        }
        catch(SQLException e) {
          /*
           * An exception caught here means that the field name isn't in the result set. Don't take
           * any action.
           */
        }

        /*
         * Only set the value in the object if there is a value with the same name in the result
         * set. This will preserve instance variables (like lists) that are assigned values when the
         * object is created.
         */
        if(Objects.nonNull(fieldValue)) {
          /*
           * Convert the following types: Time -> LocalTime, and Timestamp -> LocalDateTime.
           */
          if(fieldValue instanceof Time && fieldType.equals(LocalTime.class)) {
            fieldValue = ((Time)fieldValue).toLocalTime();
          }
          else if(fieldValue instanceof Timestamp && fieldType.equals(LocalDateTime.class)) {
            fieldValue = ((Timestamp)fieldValue).toLocalDateTime();
          }

          field.set(obj, fieldValue);
        }
      }

      return obj;

    }
    catch(Exception e) {
      throw new DaoException("Unable to create object of type " + classType.getName(), e);
    }
  }

  /**
   * This converts a camel case value (rowInsertTime) to snake case (row_insert_time).
   * 
   * @param identifier The name in camel case to convert.
   * @return The name converted to snake case.
   */
  private String camelCaseToSnakeCase(String identifier) {
    StringBuilder nameBuilder = new StringBuilder();

    for(char ch : identifier.toCharArray()) {
      if(Character.isUpperCase(ch)) {
        nameBuilder.append('_').append(Character.toLowerCase(ch));
      }
      else {
        nameBuilder.append(ch);
      }
    }

    return nameBuilder.toString();
  }

  /**
   * This class declares the exception throw by the {@link DaoBase} class. It is a thin wrapper for
   * {@link RuntimeException}.
   * 
   * @author Promineo
   *
   */
  @SuppressWarnings("serial")
  static class DaoException extends RuntimeException {

    /**
     * @param message
     * @param cause
     */
    public DaoException(String message, Throwable cause) {
      super(message, cause);
    }

    /**
     * @param message
     */
    public DaoException(String message) {
      super(message);
    }
  }
}
