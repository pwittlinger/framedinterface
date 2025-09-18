package org.framedinterface.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslationUtils {

	private TranslationUtils() {
		//Private constructor to avoid unnecessary instantiation of the class
	}

	public static void translateProblemToViolated() throws IOException {
		// Make sure problem has been generated -> exists
		// Open file path
		// 
		File f = new File("problem.pddl");
		
		// Check that file exists
		if (!f.isFile()){
			return;
		}

		// https://stackoverflow.com/questions/4716503/reading-a-plain-text-file-in-java
		Pattern pattern = Pattern.compile("\\(dummy_state\\s(s\\d+)\\)", Pattern.CASE_INSENSITIVE);
		
		try(BufferedReader br = new BufferedReader(new FileReader(f))) {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append(System.lineSeparator());
				line = br.readLine();
			}
			String fContent = sb.toString();
			
			// Setting all dummy states as final states
			// Such that the plan is still valid if it ends up in a violating state.
			String res = pattern.matcher(fContent).replaceAll(m -> m.group().replace(m.group(), m.group() +"\n\t\t(final_state "+m.group(1)+")" ));
			
			String findString = "(:goal (forall (?s - state) (imply (cur_state ?s) (final_state ?s))))";
			
			String replaceString = "(:goal (and \r\n"
					+ "		(forall (?s - state) (imply (cur_state ?s) (final_state ?s)))\r\n"
					+ "		(recovery_finished)))";
			res = res.replace(findString, replaceString);
			
			PrintWriter pwriter = new PrintWriter(new BufferedWriter(new FileWriter("problem_violated.pddl")));
			
			try {
				pwriter.println(res);
				pwriter.close();
				
			} catch (Exception e) {
				// TODO: handle exception
				System.out.println(e.getMessage());
			}
			
		}
			
	}
}