package org.framedinterface.utils.enums;

//What the data-aware planner optimizes, chosen in the Data-Aware page's planner settings. Constraint violations are always optimized
public enum OptimizationTarget {
	CONSTRAINT_VIOLATION("Constraint Violation"),
	ACTIVITY_COST("Activity Cost"), //If not optimized, the PDDL generator gets a cost model with all activity costs set to 0
	EXECUTION_TIME("Execution Time"); //If not optimized, the PDDL generator ignores the time conditions of the Declare constraints (--ignore_time_conditions)

	private final String displayName;

	private OptimizationTarget(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
}
