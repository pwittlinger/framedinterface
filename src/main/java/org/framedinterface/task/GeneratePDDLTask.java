package org.framedinterface.task;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;

import javafx.concurrent.Task;

/**
 * Generates the PDDL problem for the Data-Agnostic page by running the Framed Autonomy Tool ({@code FramedAutonomyTool.jar}), which
 * writes {@code problem.pddl} to the working directory for {@link RunPlannerTask} to solve. The tool asks two questions on its console,
 * which this task answers by watching the tool's output: the Petri net's final marking, and whether constraints are reset after a
 * violation. The task's value is the tool's exit code.
 *
 * @see GenerateDataAwarePDDLTask
 */
public class GeneratePDDLTask extends Task<Integer> {

	private ArrayList<String> commandStrings;
	private String finMarking;
	private boolean reset;

	/**
	 * Creates the task; the tool is only run once the task is started.
	 *
	 * @param commandStrings the command line that runs the tool, e.g.
	 *        {@code java -jar FramedAutonomyTool.jar <Declare model> <prefix file> <Petri net>}
	 * @param finMarking the Petri net's final marking, e.g. {@code [(960,1)]}, given when the tool asks for it
	 * @param reset the answer to the tool's question whether constraints are reset after a violation: {@code true} for yes, {@code false}
	 *        for no
	 */
	public GeneratePDDLTask(ArrayList<String> commandStrings, String finMarking, boolean reset) {
		this.commandStrings = commandStrings;
		this.finMarking = finMarking;
		this.reset = reset;
	}



	/**
	 * Runs the tool and waits for it to finish. While the tool runs, its output is echoed to FrAIm's console and watched for its two
	 * questions, which are answered with the final marking and with {@code y} or {@code n}. The output is read character by character,
	 * since the questions don't end with a line break. Reading stops once the output contains "finished" (in any case). The tool's error
	 * output goes straight to FrAIm's console.
	 *
	 * @return the tool's exit code, which is always 0 (any other exit code fails the task)
	 * @throws java.io.IOException if the tool can't be started, or reading its output or answering its questions fails
	 * @throws InterruptedException if the thread is interrupted while waiting for the tool to exit; the tool's process isn't stopped then
	 * @throws Exception if the tool exits with an error
	 */
	@Override
	protected Integer call() throws Exception {
		String domain = "n";
		if (reset) {
			domain = "y";
		}

		//Thread.sleep(3000);


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

		if (exitCode != 0) {
			System.out.println("GeneratePDDLTask failed with exit code: " + exitCode);
			throw new Exception("Generating PDDL failed. Command line exit code: "  + exitCode);
		}

		System.out.println("GeneratePDDLTask done with code: " + exitCode);
		return exitCode;
	}

}
