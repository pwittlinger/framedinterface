package org.framedinterface.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.List;


/**
 * Runs external programs. {@link #runProcess(List, File, File)} runs the Data-Aware page's PDDL generator and ENHSP. The other methods
 * are older variants for the Data-Agnostic page (running the Framed Autonomy Tool and Fast Downward), which aren't used anywhere: that
 * page uses {@link org.framedinterface.task.GeneratePDDLTask} and {@link org.framedinterface.task.RunPlannerTask} instead.
 */
public class RunnerUtils {

	/**
	 * Runs the given command to completion from the given working directory, with stdout and stderr both going to {@code outputFile} (or
	 * to this process' console if {@code outputFile} is {@code null}). If the calling thread is interrupted (e.g. a cancelled Task), the
	 * external process is destroyed rather than left running in the background. The command is printed to the console first.
	 *
	 * @param command the command and its arguments
	 * @param workingDirectory the folder to run the command from
	 * @param outputFile the file the command's output replaces the contents of, or {@code null} for this process' console
	 * @return the exit code of the process
	 * @throws IOException if the command can't be started
	 * @throws InterruptedException if the calling thread is interrupted while waiting; the process has been destroyed then
	 */
	public static int runProcess(List<String> command, File workingDirectory, File outputFile) throws IOException, InterruptedException {
		System.out.println(command);
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(workingDirectory);
		pb.redirectErrorStream(true);
		pb.redirectOutput(outputFile == null ? Redirect.INHERIT : Redirect.to(outputFile));

		Process process = pb.start();
		try {
			return process.waitFor();
		} catch (InterruptedException e) {
			process.destroyForcibly();
			throw e;
		}
	}


	/**
	 * Runs the Framed Autonomy Tool and answers its two console questions, as {@link org.framedinterface.task.GeneratePDDLTask} does,
	 * which this method duplicates. Not used anywhere in FrAIm.
	 *
	 * @param commandStrings the command line that runs the tool
	 * @param finMarking the Petri net's final marking, given when the tool asks for it
	 * @param reset the answer to the tool's question whether constraints are reset after a violation
	 * @return the tool's exit code
	 * @throws IOException if the tool can't be started, or reading its output or answering its questions fails
	 * @throws InterruptedException if the thread is interrupted while waiting for the tool to exit; the tool's process isn't stopped then
	 */
    public static int generatePDDL(ArrayList<String> commandStrings, String finMarking, boolean reset) throws IOException, InterruptedException{
        String domain = "n";
        if (reset) {
            domain = "y";
        }

        ProcessBuilder pb = new ProcessBuilder(commandStrings);
		//Process process = pb.start();
		pb.redirectError(Redirect.INHERIT);

		// https://stackoverflow.com/questions/52988779/how-do-i-automate-a-command-line-script-with-java-processbuilder/52989154
		/*
		 * I could get it to run with BufferedReader.readLine()
		 * Instead i had to switch to a read and handle it separately.
		 */
		Process process = pb.start();

		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		BufferedWriter bwriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
		BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

		StringBuilder outputBuffer = new StringBuilder();
		int ch;
		while ((ch = reader.read()) != -1) {
			char character = (char) ch;
			outputBuffer.append(character);
			System.out.print(character); // Optional: live output

			String output = outputBuffer.toString();

			if (output.contains("Insert the Petrinet final marking (e.g.: '[(960,1)]'): ")) {
				bwriter.write(finMarking);
				bwriter.newLine();
				bwriter.flush();
				outputBuffer.setLength(0); // Clear buffer
			} else if (output.contains("Do you want to reset the constraints after a violation? [y/n]")) {
				bwriter.write(domain);
				bwriter.newLine();
				bwriter.flush();
				outputBuffer.setLength(0);
			} else if (output.toLowerCase().contains("finished")) {
				break;
			}
		}

		// Read and print any remaining error output
		String errLine;
		while ((errLine = errorReader.readLine()) != null) {
			System.err.println("[STDERR] " + errLine);
		}

		int exitCode = process.waitFor();
		System.out.println("Process exited with code: " + exitCode);


        return exitCode;
    }

	/**
	 * Runs the locally installed Fast Downward ({@code fast-downward/fast-downward.py}, with {@code python.exe}) on the newly generated
	 * {@code problem.pddl}, with A* search and the blind heuristic, and writes its output to {@code results.txt}. Not used anywhere in
	 * FrAIm; {@link org.framedinterface.task.RunPlannerTask} does this for the Data-Agnostic page.
	 *
	 * @param currentPath the current directory (of the JAR file), which holds Fast Downward, the problem and the domains, and receives
	 *        {@code results.txt}
	 * @param domainReset {@code true} to plan with {@code domain_with_reset.pddl}, {@code false} with {@code domain_no_reset.pddl}
	 * @return Fast Downward's exit code
	 * @throws InterruptedException if the thread is interrupted while waiting for Fast Downward to exit; it isn't stopped then
	 * @throws IOException if Fast Downward can't be started
	 */

    public static int runPlanner(String currentPath, boolean domainReset) throws InterruptedException, IOException{
		
        	ArrayList<String> commandFastDownward = new ArrayList<String>();
            String domainPath;
			String fastDownwardPath = currentPath+"/fast-downward/fast-downward.py";
			
			String problemPath = currentPath+"/problem.pddl";
            if (domainReset){
                domainPath = currentPath+"/domain_with_reset.pddl";
            }
            else{
                domainPath = currentPath+"/domain_no_reset.pddl";
            }

			// Building the Array that will be passed to Process Builder to invoke Fast-Downward.
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

	/**
	 * Runs Fast Downward from its Apptainer image ({@code fast-downward.sif}, through WSL on Windows) on {@code problem.pddl}, like
	 * {@link #runPlanner(String, boolean)}. The image, the problem and the domains are looked for relative to FrAIm's working directory.
	 * Not used anywhere in FrAIm; {@link org.framedinterface.task.RunPlannerTask} does this for the Data-Agnostic page.
	 *
	 * @param currentPath the folder that receives {@code results.txt}
	 * @param domainReset {@code true} to plan with {@code domain_with_reset.pddl}, {@code false} with {@code domain_no_reset.pddl}
	 * @return Fast Downward's exit code
	 * @throws InterruptedException if the thread is interrupted while waiting for Fast Downward to exit; it isn't stopped then
	 * @throws IOException if Fast Downward can't be started
	 */
	public static int runPlanner2(String currentPath, boolean domainReset) throws InterruptedException, IOException{
		String os = System.getProperty("os.name");

        ArrayList<String> commandFastDownward = new ArrayList<String>();
            String domainPath;

			String problemPath = "problem.pddl";
            if (domainReset){
                
				domainPath = "domain_with_reset.pddl";
            }
            else{
                domainPath = "domain_no_reset.pddl";
            }

			
			if (os.contains("Windows")) {
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
    
}
