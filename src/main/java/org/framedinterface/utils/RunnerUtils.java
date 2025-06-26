package org.framedinterface.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;


public class RunnerUtils {
    //private static String pythonPath = "C:/Users/paulw/anaconda3/python.exe";

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

	public static int runPlanner2(String currentPath, boolean domainReset) throws InterruptedException, IOException{
		String os = System.getProperty("os.name");

        ArrayList<String> commandFastDownward = new ArrayList<String>();
            String domainPath;
			//String fastDownwardPath = currentPath+"/fast-downward/fast-downward.py";
			
			//String problemPath = currentPath+"/problem.pddl";
			String problemPath = "problem.pddl";
            if (domainReset){
                //domainPath = currentPath+"/domain_with_reset.pddl";
				domainPath = "domain_with_reset.pddl";
            }
            else{
                domainPath = "domain_no_reset.pddl";
            }

			//commandFastDownward.add(pythonPath);
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
