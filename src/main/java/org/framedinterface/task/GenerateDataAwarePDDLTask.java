package org.framedinterface.task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.framedinterface.utils.RunnerUtils;

import javafx.concurrent.Task;

/**
 * Runs the data-aware PDDL generator (https://github.com/pwittlinger/data-framed-autonomy) on an XES log, producing one
 * PDDL problem per trace in output/pddl/problem<i>.pddl (numbered from 1, in trace order).
 * The generator always writes relative to its working directory (its -o option is overridden internally), so it is run from currentPath.
 */
public class GenerateDataAwarePDDLTask extends Task<Integer> {

	public static final String PDDL_GEN_JAR = "dependencies/pddl_gen-1.0-SNAPSHOT-launcher.jar";
	public static final String OUTPUT_FOLDER = "output";
	public static final String PDDL_OUTPUT_FOLDER = OUTPUT_FOLDER + "/pddl";
	//Written by the generator alongside the problems; needed to decode the planner's output (activity and value encodings)
	public static final String VARIABLE_VALUES_FILE = OUTPUT_FOLDER + "/variable_values.txt";
	public static final String VARIABLE_SUBSTITUTIONS_FILE = OUTPUT_FOLDER + "/variable_substitutions.txt";
	public static final String COST_MODEL_FILE = OUTPUT_FOLDER + "/cost_model.txt";
	public static final String ACTIVITY_MAPPING_FILE_PATTERN = "activityMapping_.*\\.txt"; //One per model, in OUTPUT_FOLDER

	private String currentPath;
	private List<String> declPaths;
	private List<String> pnPaths;
	private String logPath;

	public GenerateDataAwarePDDLTask(String currentPath, List<String> declPaths, List<String> pnPaths, String logPath) {
		this.currentPath = currentPath;
		this.declPaths = declPaths;
		this.pnPaths = pnPaths;
		this.logPath = logPath;
	}

	@Override
	protected Integer call() throws Exception {
		File jar = new File(currentPath, PDDL_GEN_JAR);
		if (!jar.isFile()) {
			throw new Exception("PDDL generator not found: " + jar.getAbsolutePath());
		}

		//Files left over from a previous run must not be mistaken for the ones generated now. This includes the variable/cost files,
		//which the generator only creates if they don't exist yet (and would otherwise silently reuse for different models)
		deleteFiles(new File(currentPath, PDDL_OUTPUT_FOLDER), "problem\\d+\\.pddl");
		deleteFiles(new File(currentPath, OUTPUT_FOLDER), ACTIVITY_MAPPING_FILE_PATTERN);
		new File(currentPath, VARIABLE_VALUES_FILE).delete();
		new File(currentPath, VARIABLE_SUBSTITUTIONS_FILE).delete();
		new File(currentPath, COST_MODEL_FILE).delete();
		new File(currentPath, OUTPUT_FOLDER).mkdirs();

		ArrayList<String> command = new ArrayList<String>();
		command.add("java");
		command.add("-jar");
		command.add(jar.getAbsolutePath());
		command.add("-d");
		command.add(String.join(",", declPaths));
		if (!pnPaths.isEmpty()) {
			command.add("-p");
			command.add(String.join(",", pnPaths));
		}
		command.add("-l");
		command.add(logPath);
		//The generator resolves these three against its working directory itself, so they must stay relative
		command.add("-a");
		command.add(VARIABLE_VALUES_FILE);
		command.add("-s");
		command.add(VARIABLE_SUBSTITUTIONS_FILE);
		command.add("-c");
		command.add(COST_MODEL_FILE);

		int exitCode = RunnerUtils.runProcess(command, new File(currentPath), null);
		if (exitCode != 0) {
			System.out.println("GenerateDataAwarePDDLTask failed with exit code: " + exitCode);
			throw new Exception("Command line exit code: " + exitCode);
		}

		System.out.println("GenerateDataAwarePDDLTask done with code: " + exitCode);
		return exitCode;
	}

	private static void deleteFiles(File folder, String nameRegex) {
		File[] files = folder.listFiles((dir, name) -> name.matches(nameRegex));
		if (files != null) {
			for (File file : files) {
				file.delete();
			}
		}
	}

}
