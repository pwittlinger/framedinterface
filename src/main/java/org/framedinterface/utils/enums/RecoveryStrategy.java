package org.framedinterface.utils.enums;

/**
 * How the data-aware planner recovers from a violated constraint, chosen in the Data-Aware page's planner settings: passed to the PDDL
 * generator as {@code --recovery <cliValue>}, and selects the PDDL domain ENHSP plans with.
 */
public enum RecoveryStrategy {
	/** The constraint is reset to its initial state and can be violated again later; each reset is charged its violation cost. */
	RESET("Reset", "reset", "domain_framed_autonomy_process_reset_time_costs.pddl",
			"The constraint is reset to its initial state and can be violated again later."),
	/** The failure state is non-accepting and can't be left, so a violation makes the problem unsolvable. */
	NO_RECOVERY("No-Recovery", "no-recovery", "domain_framed_autonomy_process_no-recovery_time_costs.pddl",
			"The failure state is non-accepting and cannot be left, so a violation makes the problem unsolvable."),
	/**
	 * The constraint is ignored after its violation, so its failure states also become accepting; each constraint is charged its violation
	 * cost once, however often it's violated.
	 */
	DISCARD("Discard", "discard", "domain_framed_autonomy_process_discard_time_costs.pddl",
			"The constraint is ignored after its violation, so its failure states also become accepting."),
	/** Events that would violate the constraint are skipped for it, so it stays in the state before the violation. */
	SKIP("Skip", "skip", "domain_framed_autonomy_process_skip_time_costs.pddl",
			"Events that would violate the constraint are skipped for it, it stays in the state before the violation.");

	private final String displayName;
	private final String cliValue;
	private final String domainFileName; //In the dependencies folder
	private final String description;

	/**
	 * Creates a recovery strategy.
	 *
	 * @param displayName the name shown in the planner settings
	 * @param cliValue the PDDL generator's {@code --recovery} value
	 * @param domainFileName the file name of the strategy's PDDL domain
	 * @param description the explanation shown in the planner settings
	 */
	private RecoveryStrategy(String displayName, String cliValue, String domainFileName, String description) {
		this.displayName = displayName;
		this.cliValue = cliValue;
		this.domainFileName = domainFileName;
		this.description = description;
	}

	/**
	 * Returns the value the PDDL generator gets for this strategy, as {@code --recovery <value>}.
	 *
	 * @return the value, e.g. {@code no-recovery}
	 */
	public String getCliValue() {
		return cliValue;
	}

	/**
	 * Returns the file name of the strategy's PDDL domain, which is looked for in the {@code dependencies} folder (see
	 * {@link org.framedinterface.task.RunEnhspTask#getDomainFile(String, RecoveryStrategy)}).
	 *
	 * @return the file name, e.g. {@code domain_framed_autonomy_process_reset_time_costs.pddl}
	 */
	public String getDomainFileName() {
		return domainFileName;
	}

	/**
	 * Returns the explanation shown below the strategy in the planner settings.
	 *
	 * @return the explanation
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the name shown in the planner settings.
	 *
	 * @return the name, e.g. {@code No-Recovery}
	 */
	@Override
	public String toString() {
		return displayName;
	}
}
