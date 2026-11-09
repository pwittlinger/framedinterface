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

/**
 * File helpers: the file dialogs for process specifications and XES logs, and reading Fast Downward's planner output for the Data-Agnostic
 * page. All dialogs open in the folder the user last picked a file in, in any of them, for as long as FrAIm runs.
 */
public class FileUtils {

	private static File previousDirectory;

	//Extension filters for models
	private static ExtensionFilter modelExtensionFilter = new ExtensionFilter("Process specification", Arrays.asList("*.decl", "*.pnml"));
	private static ExtensionFilter xesExtensionFilter = new ExtensionFilter("XES event log", Arrays.asList("*.xes"));

	
	/**
	 * Not meant to be instantiated: all methods are static.
	 */
	private FileUtils() {
	}

	/**
	 * Reads the plan from Fast Downward's output, for the Data-Agnostic page: the lines between "Solution found!" and "Plan length: ...",
	 * without the first two (the "Solution found!" line and the search time). Also prints the plan cost to the console.
	 *
	 * @param resultFilePath the file Fast Downward's output was written to, e.g. {@code results.txt}
	 * @return one line per plan step, e.g. {@code sync activitya (0)} or {@code violate_decl s21 s22 absence_activityc (3)}
	 * @throws IndexOutOfBoundsException if the file contains no plan, or can't be read (which is printed)
	 */
	public static ArrayList<String> parsePlan(String resultFilePath){
		ArrayList<String> plan = new ArrayList<String>();
		String tCost;
		boolean addLineIntoPlan = false;
		File file = new File(resultFilePath);
		try (Scanner s = new Scanner(file)) {
			while (s.hasNextLine()) {
				String line = s.nextLine();
				if (line.matches("^\\[t=\\d+\\.\\d+[smh],\\s\\d+\\s[KMG]B\\]\\sSolution\\sfound!$")){
					//System.out.println("Solution found!: " + line);
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

	/**
	 * Reads the plan cost from Fast Downward's output, for the Data-Agnostic page, and turns it into the number of violations by dividing
	 * it by 3: it assumes that each violation step costs 3 and all other steps 0, as in e.g. {@code violate_decl s21 s22 absence_activityc (3)}.
	 *
	 * @param resultFilePath the file Fast Downward's output was written to, e.g. {@code results.txt}
	 * @return the number of violations, as text
	 * @throws NumberFormatException if the file contains no plan cost, or can't be read (which is printed)
	 */
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

		// Turning plan cost into number of violations.
		tCost = String.valueOf(Integer.parseInt(tCost) / 3);
		
		return tCost;
	}



	/**
	 * Lets the user pick one or more process specifications ({@code .decl} or {@code .pnml} files).
	 *
	 * @param stage the window the dialog belongs to
	 * @return the picked files, or {@code null} if the dialog was closed without picking any
	 */
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

	/**
	 * Lets the user choose where to save an XES log. Opens in the previously used folder if there is one, otherwise in
	 * {@code defaultDirectory} (if it exists).
	 *
	 * @param stage the window the dialog belongs to
	 * @param defaultDirectory the folder to open in if none was used before; may be {@code null}
	 * @param initialFileName the suggested file name
	 * @return the chosen file, or {@code null} if the dialog was closed without choosing one
	 */
	public static File showXesSaveDialog(Stage stage, File defaultDirectory, String initialFileName) {
		FileChooser fileChooser = new FileChooser();
		if (previousDirectory != null && previousDirectory.exists()) {
			fileChooser.setInitialDirectory(previousDirectory);
		} else if (defaultDirectory != null && defaultDirectory.isDirectory()) {
			fileChooser.setInitialDirectory(defaultDirectory);
		}
		fileChooser.setInitialFileName(initialFileName);

		fileChooser.getExtensionFilters().add(xesExtensionFilter);
		File chosenFile = fileChooser.showSaveDialog(stage);

		if (chosenFile != null) {
			previousDirectory = chosenFile.getParentFile();
		}

		return chosenFile;
	}

	/**
	 * Lets the user pick an XES log to open.
	 *
	 * @param stage the window the dialog belongs to
	 * @return the picked file, or {@code null} if the dialog was closed without picking one
	 */
	public static File showXesOpenDialog(Stage stage) {
		FileChooser fileChooser = new FileChooser();
		if (previousDirectory != null && previousDirectory.exists()) {
			fileChooser.setInitialDirectory(previousDirectory);
		}

		fileChooser.getExtensionFilters().add(xesExtensionFilter);
		File chosenFile = fileChooser.showOpenDialog(stage);

		if (chosenFile != null) {
			previousDirectory = chosenFile.getParentFile();
		}

		return chosenFile;
	}

}
