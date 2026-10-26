package org.framedinterface.utils.enums;

//How the data-aware planner recovers from a violated constraint: passed to the PDDL generator as "--recovery <cliValue>", and selects the PDDL domain ENHSP plans with
public enum RecoveryStrategy {
	RESET("Reset", "reset", "domain_framed_autonomy_process_reset_time_costs.pddl",
			"The constraint is reset to its initial state and can be violated again later."),
	NO_RECOVERY("No-Recovery", "no-recovery", "domain_framed_autonomy_process_no-recovery_time_costs.pddl",
			"The failure state is non-accepting and cannot be left, so a violation makes the problem unsolvable."),
	DISCARD("Discard", "discard", "domain_framed_autonomy_process_discard_time_costs.pddl",
			"The constraint is ignored after its violation, so its failure states also become accepting."),
	SKIP("Skip", "skip", "domain_framed_autonomy_process_skip_time_costs.pddl",
			"Events that would violate the constraint are skipped for it, it stays in the state before the violation.");

	private final String displayName;
	private final String cliValue;
	private final String domainFileName; //In the dependencies folder
	private final String description;

	private RecoveryStrategy(String displayName, String cliValue, String domainFileName, String description) {
		this.displayName = displayName;
		this.cliValue = cliValue;
		this.domainFileName = domainFileName;
		this.description = description;
	}

	public String getCliValue() {
		return cliValue;
	}

	public String getDomainFileName() {
		return domainFileName;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return displayName;
	}
}
