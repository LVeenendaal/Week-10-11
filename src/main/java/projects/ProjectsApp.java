package projects;


import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import projects.service.ProjectService;
import projects.entity.Project;

import projects.exception.DbException;

public class ProjectsApp {
	
	
		private Scanner scanner = new Scanner(System.in);
		private ProjectService projectService= new ProjectService();
		private Project curProject;
		//@formatter: off
		private List<String> operations = List.of(
				"1) add a project" + "\n" +
				" 2) list projects" + "\n" +
				" 3) Select a project"
				);
		//@formatter: on
	

	public static void main(String[] args) {
	
		new ProjectsApp().processUserSelection();
		
	}
		private void processUserSelection() {
			boolean done = false;
			
			while(!done) {
				try {
					int selection = getUserSelection();
					
					switch(selection) {
					case -1:
						done = exitMenu();
						break;
					
					case 1:
						createProject();
						break;
						
					case 2:
						listProjects();
						break;
						
					case 3:
						selectProject();
						break;
						
					default:
						System.out.println("\n" + selection + " is not a valid selection. Try again.");
						break;
					}
				}
				catch(Exception e) {
					System.out.println("\nError: " + e + " Try again.");
					e.printStackTrace();
					}
				}
			}
		private void selectProject() {
			listProjects();
			
			Integer projectId = getIntInput( "Enter a project ID to select a project");
			
			curProject = null;
			
			curProject = projectService.fetchProjectById(projectId);
		}
		private void listProjects() {
			List<Project> projects = projectService.fetchAllProjects();
			
			System.out.println("\nProjects:");
			
			projects.forEach(project -> System.out.println(" " + project.getProjectId() + ": " + project.getProjectName()));
		}
		private int getUserSelection() {
			printOperations();
			
			Integer input = getIntInput("Enter a menu selection");
			return Objects.isNull(input) ? -1 : input;
		}
		private void printOperations() {
			System.out.println("\nThese are the available selections. Press the Enter key to quit:");
			
			operations.forEach(line -> System.out.println(" " + line));
			
			if(Objects.isNull(curProject)) {
				System.out.println("\nYou are not working with a project.");
			} else {
				System.out.println("\nYou are working with project: " + curProject);
			}
		}
		private boolean exitMenu() {
			System.out.println("Exiting the menu.");
			return true;
		}
		
		private void createProject() {
			String projectName = getStringInput("Enter the project name");
			BigDecimal estimatedHours = getDecimalInput("enter the estimated hours");
			BigDecimal actualHours = getDecimalInput("Enter the actual hours");
			Integer difficulty = getIntInput("Enter the project difficulty (1-5)");
			String notes = getStringInput("Enter the project notes");
			
			Project project = new Project();
			
			project.setProjectName(projectName);
			project.setEstimatedHours(estimatedHours);
			project.setActualHours(actualHours);
			project.setDifficulty(difficulty);
			project.setNotes(notes);
			
			Project dbProject = projectService.addProject(project);
		
			System.out.println("You have successfully created project" + dbProject);
			}
		private Integer getIntInput(String prompt) {
			String input = getStringInput(prompt);
			
			if(Objects.isNull(input)) {
				return null;
			}
			
			try {
				return Integer.valueOf(input);
			}
			catch(NumberFormatException e) {
				throw new DbException(input + " is not a valid number.");
			}
		}
		
		private BigDecimal getDecimalInput(String prompt) {
			String input = getStringInput(prompt);
			
			if(Objects.isNull(input)) {
				return null;
			}
			
			try {
				return new BigDecimal(input).setScale(2);
			}
			catch(NumberFormatException e) {
				throw new DbException(input + " is not a valid decimal number.");
			}
		}
		
		
		private String getStringInput(String prompt) {
			System.out.println(prompt + ": ");
			String input = scanner.nextLine();
			return input.isBlank() ? null : input.trim();
			}
		}
