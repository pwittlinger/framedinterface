package org.framedinterface.utils.enums;

/**
 * What the data-aware planner optimizes, chosen in the Data-Aware page's planner settings. Constraint violations are always optimized.
 */
public enum OptimizationTarget {
	/** The violation costs of the soft constraints the plan violates. Always optimized. */
	CONSTRAINT_VIOLATION("Constraint Violation"),
	/**
	 * The costs of the activities the plan adds, from the Activities pane. If not optimized, the PDDL generator gets a cost model with all
	 * activity costs set to 0.
	 */
	ACTIVITY_COST("Activity Cost"),
	/**
	 * The time the plan takes, charged as waiting costs. If not optimized, the PDDL generator ignores the time conditions of the Declare
	 * constraints ({@code --ignore_time_conditions}).
	 */
	EXECUTION_TIME("Execution Time");

	private final String displayName;

	/**
	 * Creates an optimization target.
	 *
	 * @param displayName the name shown in the planner settings
	 */
	private OptimizationTarget(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Returns the name shown in the planner settings.
	 *
	 * @return the name, e.g. {@code Activity Cost}
	 */
	@Override
	public String toString() {
		return displayName;
	}
}
