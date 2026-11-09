package org.framedinterface.utils.enums;

/**
 * The search strategy of the ENHSP planner, chosen on the ENHSP tab of the Data-Aware page's planner settings and passed as
 * {@code -s <cliValue>}.
 */
public enum SearchStrategy {
	/** ENHSP's WAStar (weighted A*). Optimal: with an optimal heuristic, it finds a plan with the lowest cost. */
	WASTAR("WAStar", true),
	/** Lazy greedy best-first search. Not optimal. */
	LAZY_GBFS("lazygbfs", false),
	/** Greedy best-first search. Not optimal. */
	GBFS("gbfs", false);

	private final String cliValue;
	private final boolean optimal; //Whether the plan found has the lowest cost (given an optimal heuristic)

	/**
	 * Creates a search strategy.
	 *
	 * @param cliValue ENHSP's {@code -s} value
	 * @param optimal whether the plan found has the lowest cost (given an optimal heuristic)
	 */
	private SearchStrategy(String cliValue, boolean optimal) {
		this.cliValue = cliValue;
		this.optimal = optimal;
	}

	/**
	 * Returns the value ENHSP gets for this strategy, as {@code -s <value>}.
	 *
	 * @return the value, e.g. {@code WAStar}
	 */
	public String getCliValue() {
		return cliValue;
	}

	/**
	 * Tells whether the plan found has the lowest cost, given an optimal heuristic (see {@link SearchHeuristic#isOptimal()}).
	 *
	 * @return {@code true} for an optimal strategy
	 */
	public boolean isOptimal() {
		return optimal;
	}

	/**
	 * Returns the strategy as shown in the planner settings: its value and whether it's optimal.
	 *
	 * @return e.g. {@code gbfs (non-optimal)}
	 */
	@Override
	public String toString() {
		return cliValue + (optimal ? " (optimal)" : " (non-optimal)");
	}
}
