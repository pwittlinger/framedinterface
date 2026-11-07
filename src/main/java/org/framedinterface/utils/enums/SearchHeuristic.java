package org.framedinterface.utils.enums;

/**
 * The search heuristic of the ENHSP planner, chosen on the ENHSP tab of the Data-Aware page's planner settings and passed as
 * {@code -h <cliValue>}.
 */
public enum SearchHeuristic {
	/** No guidance (blind search). Optimal. */
	BLIND("blind", true),
	/** The additive heuristic, h_add. Not optimal. */
	HADD("hadd", false),
	/** The max heuristic, h_max. Optimal. */
	HMAX("hmax", true),
	/** ENHSP's {@code blcost} heuristic. Not optimal. */
	BLCOST("blcost", false);

	private final String cliValue;
	private final boolean optimal; //Whether an optimal search strategy still finds a plan with the lowest cost with it

	/**
	 * Creates a heuristic.
	 *
	 * @param cliValue ENHSP's {@code -h} value
	 * @param optimal whether an optimal search strategy still finds a plan with the lowest cost with this heuristic
	 */
	private SearchHeuristic(String cliValue, boolean optimal) {
		this.cliValue = cliValue;
		this.optimal = optimal;
	}

	/**
	 * Returns the value ENHSP gets for this heuristic, as {@code -h <value>}.
	 *
	 * @return the value, e.g. {@code hmax}
	 */
	public String getCliValue() {
		return cliValue;
	}

	/**
	 * Tells whether an optimal search strategy (see {@link SearchStrategy#isOptimal()}) still finds a plan with the lowest cost with this
	 * heuristic.
	 *
	 * @return {@code true} for an optimal heuristic
	 */
	public boolean isOptimal() {
		return optimal;
	}

	/**
	 * Returns the heuristic as shown in the planner settings: its value and whether it's optimal.
	 *
	 * @return e.g. {@code hadd (non-optimal)}
	 */
	@Override
	public String toString() {
		return cliValue + (optimal ? " (optimal)" : " (non-optimal)");
	}
}
