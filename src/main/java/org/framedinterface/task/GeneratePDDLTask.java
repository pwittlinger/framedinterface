package org.framedinterface.task;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;

import javafx.concurrent.Task;

public class GeneratePDDLTask extends Task<Integer> {

	private ArrayList<String> commandStrings;
	private String finMarking;
	private boolean reset;

	public GeneratePDDLTask(ArrayList<String> commandStrings, String finMarking, boolean reset) {
		this.commandStrings = commandStrings;
		this.finMarking = finMarking;
		this.reset = reset;
	}



	@Override
	protected Integer call() throws Exception {
		String domain = "n";
		if (reset) {
			domain = "y";
		}

		Thread.sleep(3000);


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
			System.out.println("GeneratePDDLTask failed with code: " + exitCode);
			throw new Exception("Generating PDDL failed. Command line exit code: "  + exitCode);
		}

		System.out.println("GeneratePDDLTask done with code: " + exitCode);
		return exitCode;
	}

}
