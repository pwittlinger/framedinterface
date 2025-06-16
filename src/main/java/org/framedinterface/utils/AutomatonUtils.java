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

public class AutomatonUtils {

	private static TreeFactory<ConjunctionTreeNode, ConjunctionTreeLeaf> treeFactory = DefaultTreeFactory.getInstance();
	private static ConjunctionFactory<? extends GroupedTreeConjunction> conjunctionFactory = GroupedTreeConjunction.getFactory(treeFactory);

	private AutomatonUtils() {
		//Private constructor to avoid unnecessary instantiation of the class
	}


	public static ExecutableAutomaton createConstraintAutomaton(DeclareConstraint declareConstraint) {
		String ltlFormula = getGenericLtlFormula(declareConstraint.getTemplate());
		try {
			Formula parsedFormula = new DefaultParser(ltlFormula).parse();
			//System.out.println("Parsed formula: " + parsedFormula);
			GroupedTreeConjunction conjunction = conjunctionFactory.instance(parsedFormula);
			DeterministicAutomaton automaton = conjunction.getAutomaton().op.determinize().op.complete();
			return new ExecutableAutomaton(automaton);
		} catch (SyntaxParserException e) {
			System.err.println("Cannot create automatn for constraint: " + declareConstraint.toString());
			e.printStackTrace();
			return null;
		}
	}


	//Gets the monitoring state that corresponds to the current state of the automaton (referred to as truthValue in original implementation)
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



	//Returns a generic LTL formula for a given Declare template
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
