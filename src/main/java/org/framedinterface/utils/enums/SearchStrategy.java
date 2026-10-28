package org.framedinterface.utils.enums;

//The search strategy of the ENHSP planner, passed as "-s <cliValue>"
public enum SearchStrategy {
	WASTAR("WAStar", true),
	LAZY_GBFS("lazygbfs", false),
	GBFS("gbfs", false);

	private final String cliValue;
	private final boolean optimal; //Whether the plan found has the lowest cost (given an optimal heuristic)

	private SearchStrategy(String cliValue, boolean optimal) {
		this.cliValue = cliValue;
		this.optimal = optimal;
	}

	public String getCliValue() {
		return cliValue;
	}

	public boolean isOptimal() {
		return optimal;
	}

	@Override
	public String toString() {
		return cliValue + (optimal ? " (optimal)" : " (non-optimal)");
	}
}
