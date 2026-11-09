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
 * Runs the ENHSP planner on the PDDL problems written by {@link GenerateDataAwarePDDLTask}: {@code output/pddl/problem1.pddl} ...
 * {@code problem<problemCount>.pddl}, one after another, with the recovery strategy's domain and the given search strategy, heuristic and
 * memory. The planner output for {@code problem<i>.pddl} is written to {@code output/plans/plan<i>.txt}. The result is the list of
 * problem numbers for which no plan was found.
 * <p>
 * Solved problems go in a cache ({@link #PLAN_CACHE}), keyed by the planner, its options, the domain and the problem. When we get the
 * exact same problem again (same model, same prefix), we take the plan from there instead of running ENHSP.
 *
 * @see org.framedinterface.utils.EnhspPlanParser
 */
public class RunEnhspTask extends Task<List<Integer>> {

	/**
	 * The ENHSP jar. {@code mvn package} copies it there (see the maven-dependency-plugin in {@code pom.xml}), after {@code mvn install}
	 * was run in {@code planner/}.
	 */
	public static final String ENHSP_JAR = "dependencies/enhsp.jar";
	/**
	 * The folder holding one PDDL domain per recovery strategy, named as {@link RecoveryStrategy#getDomainFileName()} says. If a domain
	 * isn't there, the one that comes with the generator is used (see {@link #findDomain()}).
	 */
	public static final String DOMAIN_FOLDER = "dependencies";
	/** The folder the planner output is written to, one {@code plan<i>.txt} per problem. */
	public static final String PLAN_OUTPUT_FOLDER = "output/plans";
	/**
	 * The cache of solved problems, {@code ~/.fraim/plan-cache}: the planner output for each solved problem, in a file named after its cache
	 * key (see {@link #cacheKey(byte[], List, byte[], byte[])}). It's never cleaned up; delete the folder to clear it.
	 */
	public static final Path PLAN_CACHE = Path.of(System.getProperty("user.home"), ".fraim", "plan-cache");

	private String currentPath;
	private int problemCount;
	private RecoveryStrategy recoveryStrategy;
	private SearchStrategy searchStrategy;
	private SearchHeuristic searchHeuristic;
	private int memoryGb; //Maximum Java heap of the planner (-Xmx)

	/**
	 * Creates the task; the planner is only run once the task is started.
	 *
	 * @param currentPath the folder the planner is run from, which all paths of this class are relative to (the project root)
	 * @param problemCount the number of problems to solve, {@code problem1.pddl} to {@code problem<problemCount>.pddl}
	 * @param recoveryStrategy the recovery strategy the problems were generated with, which selects the domain
	 * @param searchStrategy the planner's search strategy ({@code -s})
	 * @param searchHeuristic the planner's heuristic ({@code -h})
	 * @param memoryGb the most memory the planner may use, in GB (its maximum Java heap, {@code -Xmx})
	 */
	public RunEnhspTask(String currentPath, int problemCount, RecoveryStrategy recoveryStrategy, SearchStrategy searchStrategy, SearchHeuristic searchHeuristic, int memoryGb) {
		this.currentPath = currentPath;
		this.problemCount = problemCount;
		this.recoveryStrategy = recoveryStrategy;
		this.searchStrategy = searchStrategy;
		this.searchHeuristic = searchHeuristic;
		this.memoryGb = memoryGb;
	}

	/**
	 * Returns the given recovery strategy's domain in {@link #DOMAIN_FOLDER}, whether or not it exists.
	 *
	 * @param currentPath the project root
	 * @param recoveryStrategy the recovery strategy
	 * @return the domain file
	 */
	public static File getDomainFile(String currentPath, RecoveryStrategy recoveryStrategy) {
		return new File(currentPath, DOMAIN_FOLDER + "/" + recoveryStrategy.getDomainFileName());
	}

	/**
	 * Returns the variant of the given recovery strategy's domain that the generator writes next to the problems when some data condition
	 * uses or/not (nand clauses), whether or not it exists. Those problems only work with this variant of the domain.
	 *
	 * @param currentPath the project root
	 * @param recoveryStrategy the recovery strategy
	 * @return the domain file, e.g. {@code output/pddl/domain_framed_autonomy_process_reset_time_costs_clauses.pddl}
	 */
	public static File getClausesDomainFile(String currentPath, RecoveryStrategy recoveryStrategy) {
		String name = recoveryStrategy.getDomainFileName().replaceFirst("\\.pddl$", "_clauses.pddl");
		return new File(currentPath, GenerateDataAwarePDDLTask.PDDL_OUTPUT_FOLDER + "/" + name);
	}

	/**
	 * Solves the problems one after another, after deleting the plans left over from a previous run. A problem's plan is taken from the
	 * cache if it's there. Otherwise ENHSP is run on it, and the plan is stored in the cache if one was found. A problem counts as solved
	 * if ENHSP exits normally and its output contains "Problem Solved". ENHSP always gets {@code -dap}, plus {@code -sdac} for the Discard
	 * and Skip domains, whose violation costs are conditional.
	 * <p>
	 * The task's message says which prefix is being planned. When the task is cancelled, the running planner is stopped and the remaining
	 * problems are skipped.
	 *
	 * @return the numbers of the problems for which no plan was found, in ascending order; a missing problem file counts as unsolved too
	 * @throws InterruptedException if the task is cancelled while ENHSP runs; the ENHSP process is then destroyed
	 * @throws IOException if the domain isn't found, a file can't be read or written, or ENHSP can't be started
	 * @throws Exception if ENHSP's jar ({@link #ENHSP_JAR}) is missing
	 */
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

	/**
	 * Finds the domain to plan with: the clauses domain if the generator wrote one (see
	 * {@link #getClausesDomainFile(String, RecoveryStrategy)}), otherwise the strategy's one in {@link #DOMAIN_FOLDER}, otherwise the one
	 * that comes with the generator. That one is inside the pddl_gen jar we depend on, and is extracted to
	 * {@link GenerateDataAwarePDDLTask#PDDL_OUTPUT_FOLDER}, so it works without copying anything by hand. (The Data-Aware page currently
	 * checks for the domain in {@link #DOMAIN_FOLDER} before planning, so it never gets to this last option.)
	 *
	 * @return the domain file
	 * @throws IOException if the domain isn't found anywhere, or can't be extracted
	 */
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

	/**
	 * Computes a problem's cache key: same planner + same options + same domain + same problem = same plan.
	 *
	 * @param plannerKey the SHA-256 hash of the planner's jar
	 * @param plannerOptions the options ENHSP is run with (the memory isn't one, as it doesn't change the plan)
	 * @param domain the domain's contents
	 * @param problem the problem's contents
	 * @return the key, as a hexadecimal SHA-256 hash
	 * @throws NoSuchAlgorithmException never in practice, as every Java has SHA-256
	 */
	private static String cacheKey(byte[] plannerKey, List<String> plannerOptions, byte[] domain, byte[] problem) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.update(plannerKey);
		digest.update(String.join(" ", plannerOptions).getBytes());
		digest.update(sha256(domain));
		digest.update(sha256(problem));
		return HexFormat.of().formatHex(digest.digest());
	}

	/**
	 * Computes the SHA-256 hash of the given data.
	 *
	 * @param data the data
	 * @return the hash (32 bytes)
	 * @throws NoSuchAlgorithmException never in practice, as every Java has SHA-256
	 */
	private static byte[] sha256(byte[] data) throws NoSuchAlgorithmException {
		return MessageDigest.getInstance("SHA-256").digest(data);
	}

	/**
	 * Stores a plan in the cache. It's written to a temporary file first and then moved into place, so the cache doesn't end up with a
	 * partly written plan. The cache is only a speed-up, so if we can't write to it, we just keep going.
	 *
	 * @param plan the planner output for the problem
	 * @param cached where in the cache to store it
	 */
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
