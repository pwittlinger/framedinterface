package org.framedinterface.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;

public class FileUtils {

	private static File previousDirectory;

	//Extension filters for models
	private static ExtensionFilter modelExtensionFilter = new ExtensionFilter("Process specification", Arrays.asList("*.decl", "*.pnml"));

	
	// Private constructor to avoid unnecessary instantiation of the class
	private FileUtils() {
	}

	public static ArrayList<String> parsePlan(String resultFilePath){
		ArrayList<String> plan = new ArrayList<String>();
		String tCost;
		boolean addLineIntoPlan = false;
		File file = new File(resultFilePath);
		try (Scanner s = new Scanner(file)) {
			while (s.hasNextLine()) {
				String line = s.nextLine();
				if (line.matches("^\\[t=\\d+\\.\\d+[smh],\\s\\d+\\s[KMG]B\\]\\sSolution\\sfound!$")){
					System.out.println("Solution found!: " + line);
					addLineIntoPlan = true;
				}
				if (line.matches("^\\[t=\\d+\\.\\d+[smh],\\s\\d+\\s[KMG]B\\]\\sPlan\\slength:\\s\\d+\\sstep\\(s\\)\\.$")){
					//System.out.println("Plan over!: " + line);
					addLineIntoPlan = false;
				}
				if (addLineIntoPlan){
					plan.add(line);
				}

				if(line.matches("^\\[t=\\d+\\.\\d+[smh],\\s\\d+\\s[KMG]B\\]\\sPlan\\scost:\\s\\d+$")){
					tCost = line.split(":")[1].strip();
					System.out.println("Current Plan Cost:\t"+tCost);
				}
			}
		} catch (FileNotFoundException e) {
		
		System.err.println("Could not parse entry file!");
		e.printStackTrace();
		}
		// Removing the first two lines as they don't contain any information about the actual plan
		plan.remove(0);
		plan.remove(0);
		return plan;
	}

	public static String parsePlanCost(String resultFilePath){
		String tCost = "";
		File file = new File(resultFilePath);
		try (Scanner s = new Scanner(file)) {
			while (s.hasNextLine()) {
				String line = s.nextLine();


				if(line.matches("^\\[t=\\d+\\.\\d+[smh],\\s\\d+\\s[KMG]B\\]\\sPlan\\scost:\\s\\d+$")){
					tCost = line.split(":")[1].strip();
					System.out.println("Current Plan Cost:\t"+tCost);
				}
			}
		} catch (FileNotFoundException e) {
					System.err.println("Could not parse entry file!");
					e.printStackTrace();
		}
		
		return tCost;
	}



	public static List<File> showModelOpenDialog(Stage stage) {
		FileChooser fileChooser = new FileChooser();
		if (previousDirectory != null && previousDirectory.exists()) {
			fileChooser.setInitialDirectory(previousDirectory);
		}

		fileChooser.getExtensionFilters().add(modelExtensionFilter);
		List<File> chosenFiles = fileChooser.showOpenMultipleDialog(stage);

		if (chosenFiles != null) { // If true then the user just closed the dialog without choosing a file
			previousDirectory = chosenFiles.get(0).getParentFile();
		}

		return chosenFiles;
	}

}
