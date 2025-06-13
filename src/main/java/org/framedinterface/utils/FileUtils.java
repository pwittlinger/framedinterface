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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {

	private static File previousDirectory;

	public static Pattern solutionFoundPattern = Pattern.compile("^\\[t=\\d+\\.\\d+[smh],\\s\\d+\\s[KMG]B\\]\\sSolution\\sfound!$");
	public static Pattern planLengthPattern = Pattern.compile("^\\[t=\\d+\\.\\d+[smh],\\s\\d+\\s[KMG]B\\]\\sPlan\\slength:\\s\\d+\\sstep\\(s\\)\\.$");
	
	

	//Extension filters for models
	private static ExtensionFilter modelExtensionFilter = new ExtensionFilter("Process specification", Arrays.asList("*.decl", "*.ltl", "*.pnml"));

	//Extension filters for logs
	private static ExtensionFilter eventlogExtensionFilter = new ExtensionFilter("Log file", Arrays.asList("*.xes", "*.mxml"));



	// Private constructor to avoid unnecessary instantiation of the class
	private FileUtils() {
	}

	public static ArrayList<String> parsePlan(String resultFilePath){
		ArrayList<String> plan = new ArrayList<String>();
		boolean addLineIntoPlan = false;
		File file = new File(resultFilePath);
		try (Scanner s = new Scanner(file)) {
			int lengthIndex = -1;
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
			}
		} catch (FileNotFoundException e) {
		
		System.err.println("Could not parse entry file!");
		e.printStackTrace();
		}
		plan.remove(0);
		plan.remove(0);
		return plan;
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

	public static File showLogOpenDialog(Stage stage) {
		FileChooser fileChooser = new FileChooser();
		if (previousDirectory != null && previousDirectory.exists()) {
			fileChooser.setInitialDirectory(previousDirectory);
		}

		fileChooser.getExtensionFilters().add(eventlogExtensionFilter);
		File chosenFile = fileChooser.showOpenDialog(stage);

		if (chosenFile != null) { // If true then the user just closed the dialog without choosing a file
			previousDirectory = chosenFile.getParentFile();
		}

		return chosenFile;
	}


}
