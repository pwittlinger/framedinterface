package org.framedinterface.task;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.framedinterface.utils.RunnerUtils;

import javafx.concurrent.Task;

/**
 * Runs the ENHSP planner on output/pddl/problem1.pddl ... problem<problemCount>.pddl, one after another, writing the
 * planner output for problem<i>.pddl to output/plans/plan<i>.txt.
 * The result is the list of problem numbers for which no plan was found.
 */
public class RunEnhspTask extends Task<List<Integer>> {

	public static final String ENHSP_JAR = "dependencies/enhsp.jar";
	public static final String DOMAIN_FILE = "dependencies/domain_framed_autonomy_process_reset_time.pddl";
	public static final String PLAN_OUTPUT_FOLDER = "output/plans";

	private String currentPath;
	private int problemCount;

	public RunEnhspTask(String currentPath, int problemCount) {
		this.currentPath = currentPath;
		this.problemCount = problemCount;
	}

	@Override
	protected List<Integer> call() throws Exception {
		File jar = new File(currentPath, ENHSP_JAR);
		File domain = new File(currentPath, DOMAIN_FILE);
		if (!jar.isFile()) {
			throw new Exception("ENHSP not found: " + jar.getAbsolutePath());
		}
		if (!domain.isFile()) {
			throw new Exception("PDDL domain not found: " + domain.getAbsolutePath());
		}

		File planFolder = new File(currentPath, PLAN_OUTPUT_FOLDER);
		planFolder.mkdirs();
		File[] stalePlans = planFolder.listFiles((dir, name) -> name.matches("plan\\d+\\.txt"));
		if (stalePlans != null) {
			for (File stalePlan : stalePlans) {
				stalePlan.delete();
			}
		}

		List<Integer> unsolved = new ArrayList<Integer>();
		for (int i = 1; i <= problemCount; i++) {
			if (isCancelled()) {
				break;
			}
			updateMessage("Running planner on prefix " + i + " of " + problemCount + "...");

			File problem = new File(currentPath, GenerateDataAwarePDDLTask.PDDL_OUTPUT_FOLDER + "/problem" + i + ".pddl");
			if (!problem.isFile()) {
				System.out.println("RunEnhspTask: missing " + problem.getAbsolutePath());
				unsolved.add(i);
				continue;
			}

			ArrayList<String> command = new ArrayList<String>();
			command.add("java");
			command.add("-jar");
			command.add(jar.getAbsolutePath());
			command.add("-o");
			command.add(domain.getAbsolutePath());
			command.add("-f");
			command.add(problem.getAbsolutePath());
			command.add("-planner");
			command.add("opt-blind");

			File planFile = new File(planFolder, "plan" + i + ".txt");
			int exitCode = RunnerUtils.runProcess(command, new File(currentPath), planFile);
			boolean solved = exitCode == 0 && new String(Files.readAllBytes(planFile.toPath())).contains("Problem Solved");
			System.out.println("RunEnhspTask: problem" + i + ".pddl " + (solved ? "solved" : "not solved (exit code " + exitCode + ")"));
			if (!solved) {
				unsolved.add(i);
			}
		}
		return unsolved;
	}

}
