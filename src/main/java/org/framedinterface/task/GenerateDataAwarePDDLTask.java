package org.framedinterface.task;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.framedinterface.utils.RunnerUtils;
import org.framedinterface.utils.enums.RecoveryStrategy;

import javafx.concurrent.Task;

/**
 * Runs the <a href="https://github.com/pwittlinger/data-framed-autonomy">data-aware PDDL generator</a> on an XES log, producing one PDDL
 * problem per trace in {@code output/pddl/problem<i>.pddl} (numbered from 1, in trace order).
 * <p>
 * Besides the log, the generator gets the Declare models and Petri nets to plan with, the recovery strategy, and the activity and violation
 * costs set on the Data-Aware page, which this task writes to {@link #COST_MODEL_FILE} and {@link #VIOLATION_COST_FILE}. Next to the
 * problems, the generator writes {@link #VARIABLE_VALUES_FILE}, {@link #VARIABLE_SUBSTITUTIONS_FILE} and one activity mapping file per model
 * ({@link #ACTIVITY_MAPPING_FILE_PATTERN}); the value encodings and the activity mappings are needed to decode the planner's output.
 * <p>
 * The generator always writes relative to its working directory (its {@code -o} option is overridden internally), so it is run from
 * {@code currentPath}, which all paths of this class are relative to. The task's value is the generator's exit code.
 *
 * @see RunEnhspTask
 * @see org.framedinterface.utils.EnhspPlanParser
 */
public class GenerateDataAwarePDDLTask extends Task<Integer> {

	/**
	 * The generator's launcher jar, which bundles its dependencies. {@code mvn package} copies it there (see the maven-dependency-plugin in
	 * {@code pom.xml}), after {@code mvn install} was run in {@code data-framed-autonomy/pddl_gen}.
	 */
	public static final String PDDL_GEN_JAR = "dependencies/pddl_gen-1.0-SNAPSHOT-launcher.jar";
	/** The folder of the generator's input and output files; the problems are in its subfolder {@link #PDDL_OUTPUT_FOLDER}. */
	public static final String OUTPUT_FOLDER = "output";
	/**
	 * The folder the generator writes the PDDL problems to ({@code problem<i>.pddl}). When some data condition uses or/not, it also writes
	 * a variant of the recovery strategy's domain there (see {@link RunEnhspTask#getClausesDomainFile(String, RecoveryStrategy)}).
	 */
	public static final String PDDL_OUTPUT_FOLDER = OUTPUT_FOLDER + "/pddl";
	/**
	 * The generator's value encodings, one {@code <encoding> <value>} line per attribute value, e.g. {@code integer50 5}. Written by the
	 * generator alongside the problems, and needed to decode the attribute values in the planner's output.
	 */
	public static final String VARIABLE_VALUES_FILE = OUTPUT_FOLDER + "/variable_values.txt";
	/**
	 * The activity and attribute each of the generator's value encodings belongs to, one {@code <encoding> <activity> <attribute>} line per
	 * value, e.g. {@code integer1000 ActivityT integer}. Written by the generator alongside the problems.
	 */
	public static final String VARIABLE_SUBSTITUTIONS_FILE = OUTPUT_FOLDER + "/variable_substitutions.txt";
	/**
	 * The cost model, written by this task (rather than the generator) from the activity costs set on the Data-Aware page: one
	 * {@code <ActivityName> <activityCost> 0} line per activity. The generator uses an existing cost model instead of creating its default one.
	 */
	public static final String COST_MODEL_FILE = OUTPUT_FOLDER + "/cost_model.txt";
	/**
	 * The violation costs, written by this task from the constraint costs set on the Data-Aware page: one
	 * {@code <model file>|<constraint name> <cost>} line per Declare constraint and one {@code <model file> <cost>} line per Petri net, with
	 * {@code x} instead of the cost for a hard constraint (one that may not be violated).
	 */
	public static final String VIOLATION_COST_FILE = OUTPUT_FOLDER + "/violation_costs.txt";
	/**
	 * The names of the generator's activity mapping files in {@link #OUTPUT_FOLDER}, as a regular expression. The generator writes one per
	 * model, each line mapping an activity to its encoding, e.g. {@code ActivityG:a10}.
	 */
	public static final String ACTIVITY_MAPPING_FILE_PATTERN = "activityMapping_.*\\.txt";

	private String currentPath;
	private List<String> declPaths;
	private List<String> pnPaths;
	private String logPath;
	private RecoveryStrategy recoveryStrategy;
	private List<String> costModelLines;
	private List<String> violationCostLines;
	private String waitingCost; //Cost of waiting one time unit; null to use the generator's default
	//Drops the Declare constraints' time conditions and gives every event the timestamp 0, so time never advances (and no waiting costs are charged)
	private boolean ignoreTimeConditions;

	/**
	 * Creates the task; the generator is only run once the task is started.
	 *
	 * @param currentPath the folder the generator is run from, which all paths of this class are relative to (the project root)
	 * @param declPaths the files of the Declare models to plan with. Their order matters: the generator numbers repeated constraint names
	 *        across the models in this order
	 * @param pnPaths the files of the Petri nets to plan with, possibly none. The generator names them by position: {@code pn} for a single
	 *        net, otherwise {@code pn1}, {@code pn2}, ...
	 * @param logPath the XES log holding the prefixes, one trace per prefix
	 * @param recoveryStrategy how the planner recovers from a violated constraint
	 * @param costModelLines the lines written to {@link #COST_MODEL_FILE}
	 * @param violationCostLines the lines written to {@link #VIOLATION_COST_FILE}
	 * @param waitingCost the cost of waiting one time unit, or {@code null} to use the generator's default
	 * @param ignoreTimeConditions whether to drop the Declare constraints' time conditions and give every event the timestamp 0, so that
	 *        time never advances and no waiting costs are charged
	 */
	public GenerateDataAwarePDDLTask(String currentPath, List<String> declPaths, List<String> pnPaths, String logPath, RecoveryStrategy recoveryStrategy,
			List<String> costModelLines, List<String> violationCostLines, String waitingCost, boolean ignoreTimeConditions) {
		this.currentPath = currentPath;
		this.declPaths = declPaths;
		this.pnPaths = pnPaths;
		this.logPath = logPath;
		this.recoveryStrategy = recoveryStrategy;
		this.costModelLines = costModelLines;
		this.violationCostLines = violationCostLines;
		this.waitingCost = waitingCost;
		this.ignoreTimeConditions = ignoreTimeConditions;
	}

	/**
	 * Runs the generator and waits for it to finish. First deletes the output files left over from a previous run, so they can't be mistaken
	 * for the ones generated now, and writes the cost model and violation costs for the generator to read. The generator's console output
	 * goes to FrAIm's console.
	 *
	 * @return the generator's exit code, which is always 0 (any other exit code fails the task)
	 * @throws InterruptedException if the task is cancelled while the generator runs; the generator process is then destroyed
	 * @throws java.io.IOException if the cost files can't be written, or the generator can't be started
	 * @throws Exception if the generator's jar ({@link #PDDL_GEN_JAR}) is missing, or the generator exits with an error
	 */
	@Override
	protected Integer call() throws Exception {
		File jar = new File(currentPath, PDDL_GEN_JAR);
		if (!jar.isFile()) {
			throw new Exception("PDDL generator not found: " + jar.getAbsolutePath());
		}

		//Files left over from a previous run must not be mistaken for the ones generated now. This includes the variable files,
		//which the generator only creates if they don't exist yet (and would otherwise silently reuse for different models)
		deleteFiles(new File(currentPath, PDDL_OUTPUT_FOLDER), "problem\\d+\\.pddl");
		//Same for the clauses domain: if it's still there from an old run, RunEnhspTask would pick it up
		deleteFiles(new File(currentPath, PDDL_OUTPUT_FOLDER), "domain_.*_clauses\\.pddl");
		deleteFiles(new File(currentPath, OUTPUT_FOLDER), ACTIVITY_MAPPING_FILE_PATTERN);
		new File(currentPath, VARIABLE_VALUES_FILE).delete();
		new File(currentPath, VARIABLE_SUBSTITUTIONS_FILE).delete();
		new File(currentPath, OUTPUT_FOLDER).mkdirs();
		//The generator reads an existing cost model instead of creating its default one
		Files.write(new File(currentPath, COST_MODEL_FILE).toPath(), costModelLines);
		Files.write(new File(currentPath, VIOLATION_COST_FILE).toPath(), violationCostLines);

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
		command.add("--recovery");
		command.add(recoveryStrategy.getCliValue());
		command.add("--violation_cost_file");
		command.add(VIOLATION_COST_FILE); //Relative to the working directory, like the files above
		if (waitingCost != null) {
			command.add("--waiting_cost");
			command.add(waitingCost);
		}
		if (ignoreTimeConditions) {
			command.add("--ignore_time_conditions");
		}

		int exitCode = RunnerUtils.runProcess(command, new File(currentPath), null);
		if (exitCode != 0) {
			System.out.println("GenerateDataAwarePDDLTask failed with exit code: " + exitCode);
			throw new Exception("Command line exit code: " + exitCode);
		}

		System.out.println("GenerateDataAwarePDDLTask done with code: " + exitCode);
		return exitCode;
	}

	/**
	 * Deletes the files in the given folder whose names match the given regular expression. Does nothing if the folder doesn't exist. Files
	 * that can't be deleted (e.g. because another program has them open) are kept without notice.
	 *
	 * @param folder the folder; its subfolders aren't searched
	 * @param nameRegex the regular expression the whole file name must match
	 */
	private static void deleteFiles(File folder, String nameRegex) {
		File[] files = folder.listFiles((dir, name) -> name.matches(nameRegex));
		if (files != null) {
			for (File file : files) {
				file.delete();
			}
		}
	}

}
