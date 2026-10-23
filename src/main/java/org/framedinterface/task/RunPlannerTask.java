package org.framedinterface.task;

import java.io.File;
import java.util.ArrayList;

import javafx.concurrent.Task;

public class RunPlannerTask extends Task<Integer> {

	private String currentPath;
	private boolean domainReset;
	private boolean download;
	private boolean domainViolated;

	public RunPlannerTask(String currentPath, boolean domainReset, boolean download, boolean domainViolated) {
		this.currentPath = currentPath;
		this.domainReset = domainReset;
		this.download = download;
		this.domainViolated = domainViolated;
	}

	@Override
	protected Integer call() throws Exception {
		//Thread.sleep(3000);
		Integer exitCode = null;
		if (download) {
			exitCode = runWithoutDownload();
		} else {
			exitCode = runWithDownload();
		}

		if (exitCode != 0) {
			System.out.println("RunPlannerTask failed with code: " + exitCode);
			throw new Exception("Planning failed. Command line exit code: "  + exitCode);
		}
		System.out.println("RunPlannerTask done with code: " + exitCode);
		return exitCode;
	}

	private int runWithoutDownload() throws Exception {
		ArrayList<String> commandFastDownward = new ArrayList<String>();
		String domainPath;
		String fastDownwardPath = currentPath+"/fast-downward/fast-downward.py";

		String problemPath = currentPath+"/problem.pddl";
		if (domainReset){
			domainPath = currentPath+"/domain_with_reset.pddl";
		}
		else if (domainViolated){
			domainPath = currentPath +"/domain_violated.pddl";
			problemPath = currentPath+"/problem_violated.pddl";
		}
		else{
			domainPath = currentPath+"/domain_no_reset.pddl";
		}

		//commandFastDownward.add(pythonPath);
		commandFastDownward.add("python.exe");
		commandFastDownward.add(fastDownwardPath);
		commandFastDownward.add(domainPath);
		commandFastDownward.add(problemPath);
		commandFastDownward.add("--search");
		commandFastDownward.add("\"astar(blind())\"");

		System.out.println(commandFastDownward);
		System.out.println("Starting Planning...");
		File output = new File(currentPath+"/results.txt");

		ProcessBuilder pbPlanner = new ProcessBuilder(commandFastDownward);
		pbPlanner.redirectOutput(output);

		Process processPlanner = pbPlanner.start();
		return processPlanner.waitFor();
	}

	private Integer runWithDownload() throws Exception {
		String os = System.getProperty("os.name");
		boolean useWsl = os.contains("Windows");

		// wsl.exe re-parses its arguments through a Linux shell, where '\' is an escape
		// character (it silently eats backslashes) and Windows-style "C:\..." paths aren't
		// meaningful inside WSL anyway. Translate to the DrvFs form ("/mnt/c/...") before
		// building the command line that gets handed to WSL.
		String effectivePath = useWsl ? toWslPath(currentPath) : currentPath;

		ArrayList<String> commandFastDownward = new ArrayList<String>();
		String domainPath;
		//String fastDownwardPath = currentPath+"/fast-downward/fast-downward.py";

		//String problemPath = currentPath+"/problem.pddl";
		String problemPath = "problem.pddl";
		if (domainReset){
			domainPath = effectivePath+"/domain_with_reset.pddl";
		}
		else if (domainViolated){
			domainPath = effectivePath +"/domain_violated.pddl";
			problemPath = effectivePath+"/problem_violated.pddl";
		}
		else{
			domainPath = effectivePath+"/domain_no_reset.pddl";
		}

		//commandFastDownward.add(pythonPath);
		if (useWsl) {
			commandFastDownward.add("wsl");
		}
		commandFastDownward.add("apptainer/bin/apptainer");
		commandFastDownward.add("run");
		commandFastDownward.add("fast-downward.sif");
		commandFastDownward.add(domainPath);
		commandFastDownward.add(problemPath);
		commandFastDownward.add("--search");
		commandFastDownward.add("\"astar(blind())\"");

		System.out.println(commandFastDownward);
		System.out.println("Starting Planning...");
		File output = new File(currentPath+"/results.txt");

		ProcessBuilder pbPlanner = new ProcessBuilder(commandFastDownward);
		pbPlanner.redirectOutput(output);

		Process processPlanner = pbPlanner.start();
		return processPlanner.waitFor();
	}

	/**
	 * Converts an absolute Windows path (e.g. "C:\Users\paulw\Desktop\framedinterface")
	 * into the equivalent WSL/DrvFs path (e.g. "/mnt/c/Users/paulw/Desktop/framedinterface")
	 * so it can be passed as an argument to a process launched via "wsl".
	 */
	private static String toWslPath(String windowsPath) {
		String path = windowsPath.replace('\\', '/');
		if (path.length() >= 2 && path.charAt(1) == ':') {
			char driveLetter = Character.toLowerCase(path.charAt(0));
			path = "/mnt/" + driveLetter + path.substring(2);
		}
		return path;
	}
}
