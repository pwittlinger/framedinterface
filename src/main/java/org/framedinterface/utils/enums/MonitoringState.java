package org.framedinterface.utils.enums;

/**
 * The state a constraint is in at some point of a trace, as its automaton tells (see {@link org.framedinterface.utils.AutomatonUtils}).
 * "Possibly" means that later events can still change it; at the trace's end, possibly satisfied becomes satisfied and possibly violated
 * becomes violated. FrAIm's automata only produce {@link #SAT}, {@link #POSS_SAT}, {@link #POSS_VIOL} and {@link #VIOL}.
 */
public enum MonitoringState {
	/** The initial state of the constraint automata. Not produced by FrAIm's automata; could probably be removed. */
	INIT("init"),
	/** Permanently satisfied: no later event can violate the constraint. */
	SAT("sat"),
	/** Permanently violated: no later event can satisfy the constraint again. */
	VIOL("viol"),
	/** Possibly satisfied: satisfied now, but a later event can still violate the constraint. */
	POSS_SAT("poss.sat"),
	/** Possibly violated: violated now, but a later event can still satisfy the constraint. */
	POSS_VIOL("poss.viol"),
	/**
	 * In conflict with a set of other constraints: at least one of the conflicting constraints will be violated by the trace's end at the
	 * latest. Not produced by FrAIm's automata.
	 */
	CONFLICT("conflict");
	
	
	private final String mobuconltlName;
	
	/**
	 * Creates a monitoring state.
	 *
	 * @param mobuconltlName the name MoBuConLTL, the LTL monitor these states come from, gives the state
	 */
	private MonitoringState(String mobuconltlName) {
		this.mobuconltlName=mobuconltlName;
	}
	
	/**
	 * Returns the name MoBuConLTL gives the state.
	 *
	 * @return the name, e.g. {@code poss.sat}
	 */
	public String getMobuconltlName() {
		return mobuconltlName;
	}
	
	/**
	 * Returns the name MoBuConLTL gives the state (see {@link #getMobuconltlName()}).
	 *
	 * @return the name, e.g. {@code poss.sat}
	 */
	@Override
	public String toString() {
		return mobuconltlName;
	}
}
