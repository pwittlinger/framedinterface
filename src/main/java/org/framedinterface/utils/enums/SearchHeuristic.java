package org.framedinterface.utils.enums;

//The search heuristic of the ENHSP planner, passed as "-h <cliValue>"
public enum SearchHeuristic {
	BLIND("blind", true),
	HADD("hadd", false),
	HMAX("hmax", true),
	BLCOST("blcost", false);

	private final String cliValue;
	private final boolean optimal; //Whether an optimal search strategy still finds a plan with the lowest cost with it

	private SearchHeuristic(String cliValue, boolean optimal) {
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
