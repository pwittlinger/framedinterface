package org.framedinterface.utils;

import org.processmining.ltl2automaton.plugins.automaton.DeterministicAutomaton;
import org.processmining.ltl2automaton.plugins.automaton.State;
import org.processmining.ltl2automaton.plugins.automaton.Transition;
import org.processmining.ltl2automaton.plugins.formula.DefaultParser;
import org.processmining.ltl2automaton.plugins.formula.Formula;
import org.processmining.ltl2automaton.plugins.formula.conjunction.ConjunctionFactory;
import org.processmining.ltl2automaton.plugins.formula.conjunction.ConjunctionTreeLeaf;
import org.processmining.ltl2automaton.plugins.formula.conjunction.ConjunctionTreeNode;
import org.processmining.ltl2automaton.plugins.formula.conjunction.DefaultTreeFactory;
import org.processmining.ltl2automaton.plugins.formula.conjunction.GroupedTreeConjunction;
import org.processmining.ltl2automaton.plugins.formula.conjunction.TreeFactory;
import org.processmining.ltl2automaton.plugins.ltl.SyntaxParserException;
import org.processmining.plugins.declareminer.ExecutableAutomaton;
import org.processmining.plugins.declareminer.PossibleNodes;

import org.framedinterface.model.DeclareConstraint;
import org.framedinterface.utils.enums.DeclareTemplate;
import org.framedinterface.utils.enums.MonitoringState;

/**
 * Builds the automata that replay traces on Declare constraints, and tells the monitoring state of an automaton. A constraint's
 * automaton is built from its template's LTL formula (see {@link #getGenericLtlFormula(DeclareTemplate)}) with ProM's ltl2automaton
 * library, and executed as Declare Miner's {@link ExecutableAutomaton}. The formulas are evaluated on finite traces.
 *
 * @see DeclareConstraint
 */
public class AutomatonUtils {

	private static TreeFactory<ConjunctionTreeNode, ConjunctionTreeLeaf> treeFactory = DefaultTreeFactory.getInstance();
	private static ConjunctionFactory<? extends GroupedTreeConjunction> conjunctionFactory = GroupedTreeConjunction.getFactory(treeFactory);

	/**
	 * Not meant to be instantiated: all methods are static.
	 */
	private AutomatonUtils() {
		//Private constructor to avoid unnecessary instantiation of the class
	}


	/**
	 * Builds the automaton of the given constraint: its template's LTL formula is parsed, turned into an automaton, determinized, and
	 * completed, so that every state has a transition for every event.
	 *
	 * @param declareConstraint the constraint; only its template is used, which must not be {@code null}
	 * @return the automaton, or {@code null} if the formula can't be parsed (the error is printed). All current templates' formulas
	 *         parse.
	 */
	public static ExecutableAutomaton createConstraintAutomaton(DeclareConstraint declareConstraint) {
		String ltlFormula = getGenericLtlFormula(declareConstraint.getTemplate());
		try {
			Formula parsedFormula = new DefaultParser(ltlFormula).parse();
			//System.out.println("Parsed formula: " + parsedFormula);
			GroupedTreeConjunction conjunction = conjunctionFactory.instance(parsedFormula);
			DeterministicAutomaton automaton = conjunction.getAutomaton().op.determinize().op.complete();
			return new ExecutableAutomaton(automaton);
		} catch (SyntaxParserException e) {
			System.err.println("Cannot create automaton for constraint: " + declareConstraint.toString());
			e.printStackTrace();
			return null;
		}
	}


	/**
	 * Gets the monitoring state that corresponds to the automaton's current state (referred to as truthValue in the original
	 * implementation):
	 * <ul>
	 *   <li>{@link MonitoringState#SAT} for an accepting state that every event leads back to,</li>
	 *   <li>{@link MonitoringState#POSS_SAT} for any other accepting state,</li>
	 *   <li>{@link MonitoringState#VIOL} for a non-accepting state from which no accepting state can be reached,</li>
	 *   <li>{@link MonitoringState#POSS_VIOL} for any other non-accepting state.</li>
	 * </ul>
	 *
	 * @param constraintAutomaton the automaton
	 * @return the monitoring state; never {@link MonitoringState#INIT} or {@link MonitoringState#CONFLICT}
	 */
		public static MonitoringState getMonitoringState(ExecutableAutomaton constraintAutomaton) {
			MonitoringState monitoringState;
			PossibleNodes currentState = constraintAutomaton.currentState();

			if (currentState.isAccepting()) {
				monitoringState = MonitoringState.POSS_SAT;
				for (State state : currentState) {
					for (Transition t : state.getOutput()) {
						if (t.isAll()) {
							monitoringState = MonitoringState.SAT;
						}
					}
				}
			} else {
				monitoringState = MonitoringState.POSS_VIOL;
				for (State state : currentState) {
					if (!currentState.acceptingReachable(state)) {
						monitoringState = MonitoringState.VIOL;
					}
				}
			}

			return monitoringState;
		}



	/**
	 * Returns the generic LTL formula of a Declare template, in ltl2automaton's syntax: {@code <>} eventually, {@code []} always,
	 * {@code X} next, {@code U} until, {@code /\} and, {@code \/} or, {@code !} not, {@code ->} implies. "A" and "B" stand for the
	 * constraint's first and second activity as written in the model file, e.g. {@code Response[A, B]}; for the templates that reverse
	 * activation and target, "B" is the activation (see {@link DeclareTemplate#getReverseActivationTarget()}). Several negative templates
	 * mean the same and share a formula, e.g. Not Response, Not Precedence and Not Succession.
	 *
	 * @param declareTemplate the template; must not be {@code null}
	 * @return the formula; empty for a template without one, which none of the current templates is
	 */
	public static String getGenericLtlFormula(DeclareTemplate declareTemplate) {
		String formula = "";
		switch (declareTemplate) {
		case Absence:
			formula = "!( <> (  A  ) )";
			break;
		case Absence2:
			formula = "! ( <> ( (  A  /\\ X(<>( A )) ) ) )";
			break;
		case Absence3:
			formula = "! ( <> ( (  A  /\\  X ( <> (( A  /\\  X ( <> (  A  ) )) ) ) ) ))";
			break;
		case Alternate_Precedence:
			formula = "(((( !( B ) U  A ) \\/ []( !( B ))) /\\ [](( B  ->( (!(X( A )) /\\ !(X(!( A ))) ) \\/ X((( !( B ) U  A ) \\/ []( !( B )))))))) /\\ (  ! ( B  ) \\/ (!(X( A )) /\\ !(X(!( A ))) ) ))";
			break;
		case Alternate_Response:
			formula = "( []( (  A  -> X(( (! (  A  )) U  B  ) )) ) )";
			break;
		case Alternate_Succession:
			formula = "( [](( A  -> X(( !( A ) U  B )))) /\\ (((( !( B ) U  A ) \\/ []( !( B ))) /\\ [](( B  ->( (!(X( A )) /\\ !(X(!( A ))) ) \\/ X((( !( B ) U  A ) \\/ []( !( B )))))))) /\\ (  ! ( B  ) \\/ (!(X( A )) /\\ !(X(!( A ))) ) )))";
			break;
		case Chain_Precedence:
			formula = "[]( ( X(  B  ) ->  A ) )/\\ (  ! ( B  ) \\/ (!(X( A )) /\\ !(X(!( A ))) ) )";
			break;
		case Chain_Response:
			formula = "[] ( (  A  -> X(  B  ) ) )";
			break;
		case Chain_Succession:
			formula = "([]( (  A  -> X(  B  ) ) )) /\\ ([]( ( X(  B  ) ->   A ) ) /\\ (  ! ( B  ) \\/ (!(X( A )) /\\ !(X(!( A ))) ) ))";
			break;
		case Choice:
			formula = "(  <> (  A  ) \\/ <>(  B  )  )";
			break;
		case CoExistence:
			formula = "( ( <>( A ) -> <>(  B  ) ) /\\ ( <>( B ) -> <>(  A  ) )  )";
			break;
		case End:
			//formula = "( [](( A ) -> ( !(X( A  /\\ (!( A )))) )";
			//formula = "( A ) && !X (true)";
			//formula = "<>( ( A ) && !X (true))";
			//formula = "( <>(( A ) && ( ! (X( A  /\\  (!( A )))))) )";
			//formula = " ( <>(( A ) && ( (X( A  /\\  (!( A )))))) )";

			formula = "( <> (  A  && !X(  A  U ( ! A  ) ) ) )";

			break;
		case Exactly1:
			formula = "(  <> ( A ) /\\ ! ( <> ( (  A  /\\ X(<>( A )) ) ) ) )";
			break;
		case Exactly2:
			formula = "( <> ( A  /\\ ( A  -> (X(<>( A ))))) /\\  ! ( <>(  A  /\\ ( A  -> X( <>(  A  /\\ ( A  -> X ( <> (  A  ) ))) ) ) ) ) )";
			break;
		case Exclusive_Choice:
			formula = "(  ( <>(  A  ) \\/ <>(  B  )  )  /\\ !( (  <>(  A  ) /\\ <>(  B  ) ) ) )";
			break;
		case Existence:
			formula = "( <> (  A  ) )";
			break;
		case Existence2:
			formula = "<> ( (  A  /\\ X(<>( A )) ) )";
			break;
		case Existence3:
			formula = "<>(  A  /\\ X(  <>(  A  /\\ X( <>  A  )) ))";
			break;
		case Init:
			formula = "(  A  )";
			break;
		case Not_Chain_Precedence:
			formula = "[] (  A  -> !( X (  B  ) ) )";
			break;
		case Not_Chain_Response:
			formula = "[] (  A  -> !( X (  B  ) ) )";
			break;
		case Not_Chain_Succession:
			formula = "[]( (  A  -> !(X(  B  ) ) ))";
			break;
		case Not_CoExistence:
			formula = "(<>(  A  )) -> (!(<>(  B  )))";
			break;
		case Not_Precedence:
			formula = "[] (  A  -> !( <> (  B  ) ) )";
			break;
		case Not_Responded_Existence:
			formula = "(<>(  A  )) -> (!(<>(  B  )))";
			break;
		case Not_Response:
			formula = "[] (  A  -> !( <> (  B  ) ) )";
			break;
		case Not_Succession:
			formula = "[]( (  A  -> !(<>(  B  ) ) ))";
			break;
		case Precedence:
			formula = "( ! ( B  ) U  A  ) \\/ ([](!( B ))) /\\ (  ! ( B  ) \\/ (!(X( A )) /\\ !(X(!( A ))) ) )";
			break;
		case Responded_Existence:
			formula = "(( ( <>(  A  ) -> (<>(  B  ) )) ))";
			break;
		case Response:
			formula = "( []( (  A  -> <>(  B  ) ) ))";
			break;
		case Succession:
			formula = "(( []( (  A  -> <>(  B  ) ) ))) /\\ (( ! ( B  ) U  A  ) \\/ ([](!( B ))) /\\ (  ! ( B  ) \\/ (!(X( A )) /\\ !(X(!( A ))) ) )   )";
			break;
		default:
			break;
		}
		return formula;
	}

}
