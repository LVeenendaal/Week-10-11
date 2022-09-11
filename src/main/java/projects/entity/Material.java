/**
 * 
 */
package projects.entity;

import java.math.BigDecimal;

/**
 * @author Promineo
 *
 */
public class Material {
  private Integer materialId;
  private Integer projectId;
  private String materialName;
  private Integer numRequired;
  private BigDecimal cost;

  public Integer getMaterialId() {
    return materialId;
  }

  public void setMaterialId(Integer materialId) {
    this.materialId = materialId;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public String getMaterialName() {
    return materialName;
  }

  public void setMaterialName(String materialName) {
    this.materialName = materialName;
  }

  public Integer getNumRequired() {
    return numRequired;
  }

  public void setNumRequired(Integer numRequired) {
    this.numRequired = numRequired;
  }

  public BigDecimal getCost() {
    return cost;
  }

  public void setCost(BigDecimal cost) {
    this.cost = cost;
  }

  @Override
  public String toString() {
    return "ID=" + materialId + ", materialName=" + materialName + ", numRequired=" + numRequired
        + ", cost=" + cost;
  }
}
