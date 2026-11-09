package org.framedinterface.utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Parses command-line arguments with Apache Commons CLI: a file listing the input models with their violation costs ({@code -c}), an
 * event log ({@code -l}) and an optional statistics file ({@code -statsFile}).
 * <p>
 * This class isn't used anywhere in FrAIm: nothing calls {@link #handleArgs(String[])}, and its usage text names another tool's jar
 * ({@code InterplayMonitor.jar}). It has been unchanged since the project's first commit.
 */
public class CmdArgsUtil {

	/**
	 * Not meant to be instantiated: all methods are static.
	 */
	private CmdArgsUtil() {
		//Private constructor to avoid unnecessary instantiation of the class
	}

	/**
	 * Parses the given command-line arguments. If they're invalid (e.g. a required option is missing), prints the problem and the usage,
	 * and exits the JVM with status 1.
	 * <ul>
	 *   <li>{@code -c}, {@code --costsFile} (required): a file listing the input models and their violation costs, one model per line, as
	 *       the model file and its violation cost separated by a comma. The file extension gives the model type: {@code decl} for Declare,
	 *       {@code pnml} for a data Petri net, {@code ltl} for a Declare-like file with raw LTL formulas instead of templates.</li>
	 *   <li>{@code -l}, {@code --eventLog} (required): the event log.</li>
	 *   <li>{@code -statsFile} (optional): a file the main statistics are appended to.</li>
	 * </ul>
	 * Parsing stops at the first argument that isn't an option; it and the ones after it are left in the result's argument list.
	 *
	 * @param args the command-line arguments
	 * @return the parsed arguments
	 */
	public static CommandLine handleArgs(String[] args) {
		//TODO Review after implementation is done
		Options options = new Options();
		Option declareParam = new Option("c", "costsFile", true, "Input model files and violation costs. Model and corresponding violation cost separated by comma. Each model on separate line. File extension defines the model type (decl - Declare; pnml - Data Petri Net; ltl - same format as decl file, but using raw formulas instead of Declare templates) ");
		declareParam.setRequired(true);
		options.addOption(declareParam);

		Option logParam = new Option("l", "eventLog", true, "Input event log path");
		logParam.setRequired(true);
		options.addOption(logParam);

		Option statsParam = new Option("statsFile", true, "Main statistics will be appended at the end of this file (optional)");
		statsParam.setRequired(false);
		options.addOption(statsParam);

		CommandLineParser parser = new org.apache.commons.cli.DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd = null;

		try {
			cmd = parser.parse(options, args, true);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("java -jar .\\InterplayMonitor.jar ", options);
			System.exit(1);
		}

		return cmd;
	}
}
