package org.framedinterface.task;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.framedinterface.utils.RunnerUtils;
import org.framedinterface.utils.enums.RecoveryStrategy;
import org.framedinterface.utils.enums.SearchHeuristic;
import org.framedinterface.utils.enums.SearchStrategy;

import javafx.concurrent.Task;

/**
 * Runs the ENHSP planner, with the recovery strategy's domain and the given search strategy, heuristic and memory, on output/pddl/problem1.pddl ...
 * problem<problemCount>.pddl, one after another, writing the planner output for problem<i>.pddl to output/plans/plan<i>.txt.
 * The result is the list of problem numbers for which no plan was found.
 *
 * Solved problems go in a cache (~/.fraim/plan-cache), keyed by the domain, the problem and the planner. When we get
 * the exact same problem again (same model, same prefix) we take the plan from there instead of running ENHSP.
 */
public class RunEnhspTask extends Task<List<Integer>> {

	public static final String ENHSP_JAR = "dependencies/enhsp.jar";
	public static final String DOMAIN_FOLDER = "dependencies"; //Holds one PDDL domain per recovery strategy (see RecoveryStrategy.getDomainFileName)
	public static final String PLAN_OUTPUT_FOLDER = "output/plans";
	public static final Path PLAN_CACHE = Path.of(System.getProperty("user.home"), ".fraim", "plan-cache");

	private String currentPath;
	private int problemCount;
	private RecoveryStrategy recoveryStrategy;
	private SearchStrategy searchStrategy;
	private SearchHeuristic searchHeuristic;
	private int memoryGb; //Maximum Java heap of the planner (-Xmx)

	public RunEnhspTask(String currentPath, int problemCount, RecoveryStrategy recoveryStrategy, SearchStrategy searchStrategy, SearchHeuristic searchHeuristic, int memoryGb) {
		this.currentPath = currentPath;
		this.problemCount = problemCount;
		this.recoveryStrategy = recoveryStrategy;
		this.searchStrategy = searchStrategy;
		this.searchHeuristic = searchHeuristic;
		this.memoryGb = memoryGb;
	}

	public static File getDomainFile(String currentPath, RecoveryStrategy recoveryStrategy) {
		return new File(currentPath, DOMAIN_FOLDER + "/" + recoveryStrategy.getDomainFileName());
	}

	//Written by the generator next to the problems when some data condition uses or/not (nand clauses).
	//Those problems only work with this variant of the strategy's domain.
	public static File getClausesDomainFile(String currentPath, RecoveryStrategy recoveryStrategy) {
		String name = recoveryStrategy.getDomainFileName().replaceFirst("\\.pddl$", "_clauses.pddl");
		return new File(currentPath, GenerateDataAwarePDDLTask.PDDL_OUTPUT_FOLDER + "/" + name);
	}

	@Override
	protected List<Integer> call() throws Exception {
		File jar = new File(currentPath, ENHSP_JAR);
		if (!jar.isFile()) {
			throw new Exception("ENHSP not found: " + jar.getAbsolutePath() + " (run mvn install in planner/ and then mvn package here)");
		}
		File domain = findDomain();
		System.out.println("RunEnhspTask: using domain " + domain.getAbsolutePath());

		//-dap is always passed, -sdac only for the domains with conditional violation costs. The memory isn't a planner option, as it doesn't change the plan
		//(and so isn't part of the cache key either)
		List<String> plannerOptions = new ArrayList<String>(List.of("-s", searchStrategy.getCliValue(), "-h", searchHeuristic.getCliValue(), "-dap"));
		if (recoveryStrategy.getCliValue().contains("discard") | recoveryStrategy.getCliValue().contains("skip")) {
			plannerOptions.add("-sdac");
		}

		File planFolder = new File(currentPath, PLAN_OUTPUT_FOLDER);
		planFolder.mkdirs();
		File[] stalePlans = planFolder.listFiles((dir, name) -> name.matches("plan\\d+\\.txt"));
		if (stalePlans != null) {
			for (File stalePlan : stalePlans) {
				stalePlan.delete();
			}
		}

		byte[] plannerKey = sha256(Files.readAllBytes(jar.toPath()));
		byte[] domainBytes = Files.readAllBytes(domain.toPath());

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

			File planFile = new File(planFolder, "plan" + i + ".txt");
			Path cached = PLAN_CACHE.resolve(cacheKey(plannerKey, plannerOptions, domainBytes, Files.readAllBytes(problem.toPath())) + ".txt");
			if (Files.isRegularFile(cached)) {
				Files.copy(cached, planFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				System.out.println("RunEnhspTask: problem" + i + ".pddl solved (from the cache)");
				continue;
			}

			ArrayList<String> command = new ArrayList<String>();
			command.add("java");
			command.add("-jar");
			command.add("-Xmx" + memoryGb + "G");
			command.add(jar.getAbsolutePath());
			command.add("-o");
			command.add(domain.getAbsolutePath());
			command.add("-f");
			command.add(problem.getAbsolutePath());
			command.addAll(plannerOptions);

			int exitCode = RunnerUtils.runProcess(command, new File(currentPath), planFile);
			boolean solved = exitCode == 0 && new String(Files.readAllBytes(planFile.toPath())).contains("Problem Solved");
			System.out.println("RunEnhspTask: problem" + i + ".pddl " + (solved ? "solved" : "not solved (exit code " + exitCode + ")"));
			if (solved) {
				storeInCache(planFile.toPath(), cached);
			} else {
				unsolved.add(i);
			}
		}
		return unsolved;
	}

	//The clauses domain if the generator wrote one, otherwise the strategy's one in dependencies/, otherwise the one
	//that comes with the generator (it's inside the pddl_gen jar we depend on), so it works without copying anything by hand
	private File findDomain() throws IOException {
		File clausesDomain = getClausesDomainFile(currentPath, recoveryStrategy);
		if (clausesDomain.isFile()) {
			return clausesDomain;
		}
		File domain = getDomainFile(currentPath, recoveryStrategy);
		if (domain.isFile()) {
			return domain;
		}
		File extracted = new File(currentPath, GenerateDataAwarePDDLTask.PDDL_OUTPUT_FOLDER + "/" + domain.getName());
		try (InputStream in = RunEnhspTask.class.getResourceAsStream("/domains/" + domain.getName())) {
			if (in == null) {
				throw new IOException("PDDL domain not found: " + domain.getAbsolutePath());
			}
			extracted.getParentFile().mkdirs();
			Files.copy(in, extracted.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		return extracted;
	}

	//Same planner + same options + same domain + same problem = same plan
	private static String cacheKey(byte[] plannerKey, List<String> plannerOptions, byte[] domain, byte[] problem) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.update(plannerKey);
		digest.update(String.join(" ", plannerOptions).getBytes());
		digest.update(sha256(domain));
		digest.update(sha256(problem));
		return HexFormat.of().formatHex(digest.digest());
	}

	private static byte[] sha256(byte[] data) throws NoSuchAlgorithmException {
		return MessageDigest.getInstance("SHA-256").digest(data);
	}

	//The cache is only a speed-up, so if we can't write to it we just keep going
	private static void storeInCache(Path plan, Path cached) {
		try {
			Files.createDirectories(cached.getParent());
			Path temporary = Files.createTempFile(cached.getParent(), "plan", ".tmp");
			Files.copy(plan, temporary, StandardCopyOption.REPLACE_EXISTING);
			Files.move(temporary, cached, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			System.out.println("RunEnhspTask: could not cache the plan: " + e.getMessage());
		}
	}

}
