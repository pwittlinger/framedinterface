package org.framedinterface.utils;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.collections4.BidiMap;

import org.framedinterface.model.DeclareConstraint;
import org.framedinterface.utils.enums.MonitoringState;

/**
 * Builds the Graphviz DOT statements of a Declare model's visualization: a node per activity and an edge per binary constraint, colored by
 * the constraints' monitoring states (see {@link org.framedinterface.model.DeclareModel#getVisualisationString(int, boolean, boolean)}).
 * The colors are those of the Declare viewer's legend: {@code #79a888} temporarily (possibly) satisfied, {@code #66ccff} permanently
 * satisfied, {@code #ffd700} temporarily violated, {@code #d44942} permanently violated, plus {@code #ff9900} for a conflict.
 * <p>
 * Activity names are put into the labels as they are, without escaping. A name containing {@code "}, or, for an activity with unary
 * constraints, {@code &}, {@code <} or {@code >}, makes the graph invalid, so the viewer can't show the model.
 */
public class GraphGenerator {

	/**
	 * Not meant to be instantiated: all methods are static.
	 */
	private GraphGenerator() {
		//Private constructor to avoid unnecessary instantiation of the class
	}


	/**
	 * Builds an activity's node. Without unary constraints, it's a plain blue box with the activity's name. With unary constraints (e.g.
	 * Existence or Init), it's an HTML-like table: a row per unary constraint, colored by its monitoring state and showing its template
	 * (and its conditions, if shown), above a blue row with the activity's name.
	 *
	 * @param nodeId the node's id, i.e. the activity's encoding, e.g. {@code n0}
	 * @param activityName the name shown on the node; not escaped (see the class description)
	 * @param monitoringStates each constraint's monitoring state at the position being shown
	 * @param activityUnaryConstraints the activity's unary constraints; {@code null} or empty for none
	 * @param showDataConditions whether to show the unary constraints' data and time conditions
	 * @return the node's DOT statement
	 */
	public static String buildDeclNodeString(String nodeId, String activityName, Map<DeclareConstraint, MonitoringState> monitoringStates, List<DeclareConstraint> activityUnaryConstraints, boolean showDataConditions) {
		StringBuilder sb = new StringBuilder(nodeId);

		if(activityUnaryConstraints == null || activityUnaryConstraints.isEmpty()) {
			//A normal graphviz node is used if there are no unary constraints for this activity
			sb.append(" [label=" + "\"").append(activityName).append("\\\\n\"");
			sb.append(" fillcolor=\"#0000ff\" fontcolor=\"#ffffff\"");
			sb.append(" tooltip=\"").append(activityName).append("\"]");
			return sb.toString();
		} else {
			//An html table is used as the graphviz node label if there are any unary constraints for this activity
			sb.append(" [shape=none, margin=0, label=<<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">");

			for (DeclareConstraint unaryConstraint : activityUnaryConstraints) {
				sb.append("<tr><td width=\"60\" bgcolor=\"");
				MonitoringState monitoringState = monitoringStates.get(unaryConstraint);
				String unaryLabel = unaryConstraint.getTemplate().getTemplateName() + escapeHtml(getDataConditionsLabel(unaryConstraint, showDataConditions));
				//Coloring based on the constraint status
				if (monitoringState == MonitoringState.CONFLICT) {
					sb.append("#ff9900\">").append(unaryLabel);
				} else if (monitoringState == MonitoringState.SAT) {
					sb.append("#66ccff\">").append(unaryLabel);
				} else if (monitoringState == MonitoringState.VIOL) {
					sb.append("#d44942\">").append(unaryLabel);
				} else if (monitoringState == MonitoringState.POSS_VIOL) {
					sb.append("#ffd700\">").append(unaryLabel);
				} else if (monitoringState == MonitoringState.POSS_SAT) {
					sb.append("#79a888\">").append(unaryLabel);
				} else {
					sb.append("#000000\"><font color=\"white\">").append(unaryLabel).append("</font>"); //Fallback for unknown monitoring states
				}
				sb.append("</td></tr>");
			}

			sb.append("<tr><td bgcolor=\"blue\"><font color=\"white\">");
			sb.append(activityName);
			sb.append("</font></td></tr></table>>]");

			return sb.toString();
		}
	}

	/**
	 * Builds the edge of a binary constraint, from its first to its second activity as written in the model file (for the templates that
	 * reverse activation and target, that's from the target to the activation). The edge's look shows the template, and its color the
	 * monitoring state (see {@link #getBinaryConstraintStyle(DeclareConstraint, MonitoringState, String, boolean)}).
	 *
	 * @param binaryConstraint the constraint
	 * @param monitoringState the constraint's monitoring state at the position being shown
	 * @param activityToEncodingMap each activity's encoding, i.e. its node's id
	 * @param showDataConditions whether to show the constraint's data and time conditions in the edge's label
	 * @return the edge's DOT statement
	 */
	public static String buildDeclEdgeString(DeclareConstraint binaryConstraint, MonitoringState monitoringState, BidiMap<String, String> activityToEncodingMap, boolean showDataConditions) {
		StringBuilder sb = new StringBuilder();
		if (binaryConstraint.getTemplate().getReverseActivationTarget()) {
			sb.append(activityToEncodingMap.get(binaryConstraint.getTargetActivity()));
			sb.append(" -> ");
			sb.append(activityToEncodingMap.get(binaryConstraint.getActivationActivity()));
		} else {
			sb.append(activityToEncodingMap.get(binaryConstraint.getActivationActivity()));
			sb.append(" -> ");
			sb.append(activityToEncodingMap.get(binaryConstraint.getTargetActivity()));
		}

		sb.append(" ").append(getBinaryConstraintStyle(binaryConstraint, monitoringState, "", showDataConditions));
		return sb.toString();
	}

	/**
	 * Builds the {@code [activationCondition][targetCondition][timeCondition]} suffix shown when data conditions are toggled on. The target
	 * condition is omitted for unary constraints, which have no target activity.
	 *
	 * @param constraint the constraint
	 * @param showDataConditions whether data conditions are toggled on
	 * @return the suffix, e.g. {@code [A.integer > 10][T.integer < 10][]}; empty if data conditions are toggled off
	 */
	private static String getDataConditionsLabel(DeclareConstraint constraint, boolean showDataConditions) {
		if (!showDataConditions) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(constraint.getActivationCondition()).append("]");
		if (constraint.getTemplate().getIsBinary()) {
			sb.append("[").append(constraint.getTargetCondition()).append("]");
		}
		sb.append("[").append(constraint.getTimeCondition()).append("]");
		return sb.toString();
	}

	/**
	 * Escapes {@code &}, {@code <} and {@code >} for a node label with unary constraints. Those labels use Graphviz's HTML-like label
	 * syntax, which is parsed as XML: a raw {@code <} or {@code >} in a data condition (e.g. {@code T.integer < 10}) would otherwise be
	 * read as a tag.
	 *
	 * @param s the text
	 * @return the escaped text
	 */
	private static String escapeHtml(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}


	/**
	 * Builds the attributes of a binary constraint's edge. Its arrowheads, line style and number of lines depend on the template, following
	 * the usual Declare notation: e.g. negative templates are dashed, alternate templates have two lines and chain templates three. Its
	 * color depends on the monitoring state. The label is the template's name, plus the constraint's conditions if they're shown.
	 *
	 * @param binaryConstraint the constraint
	 * @param monitoringState the constraint's monitoring state; black for {@code null} or an unknown state
	 * @param penwidth further attributes to add as they are, e.g. {@code penwidth=2}; empty for none
	 * @param showDataConditions whether to add the constraint's data and time conditions to the label
	 * @return the attribute list, in square brackets
	 * @throws NoSuchElementException if the template isn't binary; every binary template has a style
	 */
	private static String getBinaryConstraintStyle(DeclareConstraint binaryConstraint, MonitoringState monitoringState, String penwidth, boolean showDataConditions) {
		String color = "#000000"; //Used if states are not shown or if the constraints has an unknown monitoring status

		//Coloring based on the constraint status
		if (monitoringState == MonitoringState.CONFLICT) {
			color = "#ff9900";
		} else if (monitoringState == MonitoringState.SAT) {
			color = "#66ccff";
		} else if (monitoringState == MonitoringState.VIOL) {
			color = "#d44942";
		} else if (monitoringState == MonitoringState.POSS_VIOL) {
			color = "#ffd700";
		} else if (monitoringState == MonitoringState.POSS_SAT) {
			color = "#79a888";
		} 


		String label = binaryConstraint.getTemplate().getTemplateName() + (showDataConditions ? "\\\\n" + getDataConditionsLabel(binaryConstraint, true) : "");
		switch(binaryConstraint.getTemplate()) {
		case Responded_Existence:
			return "[dir=\"both\", edgetooltip=\"Responded Existence\", labeltooltip=\"Responded Existence\",arrowhead=\"none\",arrowtail=\"dot\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		case Response:
			return "[dir=\"both\", edgetooltip=\"Response\", labeltooltip=\"Response\", arrowhead=\"normal\", arrowtail=\"dot\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		case Alternate_Response:
			return "[edgetooltip=\"Alternate Response\", labeltooltip=\"Alternate Response\", dir=\"both\", arrowhead=\"normal\", arrowtail=\"dot\", label=\""+label+"\", color=\""+color+":"+color+"\","+penwidth+"]";
		case Chain_Response:
			return "[edgetooltip=\"Chain Response\", labeltooltip=\"Chain Response\", dir=\"both\", arrowhead=\"normal\", arrowtail=\"dot\", label=\""+label+"\", color=\""+color+":"+color+":"+color+"\","+penwidth+"]";
		case Precedence:
			return "[arrowhead=\"dotnormal\", edgetooltip=\"Precedence\", labeltooltip=\"Precedence\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		case Alternate_Precedence:
			return "[arrowhead=\"dotnormal\", edgetooltip=\"Alternate Precedence\", labeltooltip=\"Alternate Precedence\", label=\""+label+"\", color=\""+color+":"+color+"\","+penwidth+"]";
		case Chain_Precedence:
			return "[arrowhead=\"dotnormal\", edgetooltip=\"Chain Precedence\", labeltooltip=\"Chain Precedence\", label=\""+label+"\", color=\""+color+":"+color+":"+color+"\","+penwidth+"]";
		case Succession:
			return "[dir=\"both\", edgetooltip=\"Succession\", labeltooltip=\"Succession\", arrowhead=\"dotnormal\", arrowtail=\"dot\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		case Alternate_Succession:
			return "[dir=\"both\", edgetooltip=\"Alternate Succession\", labeltooltip=\"Alternate Succession\", arrowhead=\"dotnormal\", arrowtail=\"dot\", label=\""+label+"\", color=\""+color+":"+color+"\","+penwidth+"]";
		case Chain_Succession:
			return "[dir=\"both\", edgetooltip=\"Chain Succession\", labeltooltip=\"Chain Succession\", arrowhead=\"dotnormal\", arrowtail=\"dot\", label=\""+label+"\", color=\""+color+":"+color+":"+color+"\","+penwidth+"]";
		case CoExistence:
			return "[dir=\"both\", edgetooltip=\"CoExistence\", labeltooltip=\"CoExistence\", arrowhead=\"dot\", arrowtail=\"dot\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		case Choice:
			return "[dir=\"both\", edgetooltip=\"Choice\", labeltooltip=\"Choice\", arrowhead=\"odiamond\", arrowtail=\"odiamond\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		case Exclusive_Choice:
			return "[dir=\"both\", edgetooltip=\"Exclusive Choice\", labeltooltip=\"Exclusive Choice\", arrowhead=\"diamond\", arrowtail=\"diamond\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		case Not_Chain_Precedence:
			return "[arrowhead=\"dotnormal\", edgetooltip=\"Not Chain Precedence\", labeltooltip=\"Not Chain Precedence\", style=\"dashed\", label=\""+label+"\", color=\""+color+":"+color+":"+color+"\","+penwidth+"]";
		case Not_Chain_Response:
			return "[dir=\"both\", edgetooltip=\"Not Chain Response\", labeltooltip=\"Not Chain Response\", arrowhead=\"normal\", arrowtail=\"dot\", style=\"dashed\", label=\""+label+"\", color=\""+color+":"+color+":"+color+"\","+penwidth+"]";
		case Not_Chain_Succession:
			return "[dir=\"both\", edgetooltip=\"Not Chain Succession\", labeltooltip=\"Not Chain Succession\", arrowhead=\"dotnormal\", arrowtail=\"dot\", style=\"dashed\", label=\""+label+"\", color=\""+color+":"+color+":"+color+"\","+penwidth+"]";
		case Not_CoExistence:
			return "[dir=\"both\", edgetooltip=\"Not CoExistence\", labeltooltip=\"Not CoExistence\", arrowhead=\"dot\", arrowtail=\"dot\", style=\"dashed\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		case Not_Precedence:
			return "[arrowhead=\"dotnormal\", edgetooltip=\"Not Precedence\", labeltooltip=\"Not Precedence\", style=\"dashed\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		case Not_Responded_Existence:
			return "[dir=\"both\", arrowtail=\"dot\", arrowhead=\"none\", edgetooltip=\"Not Responded Existence\", labeltooltip=\"Not Responded Existence\", style=\"dashed\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		case Not_Response:
			return "[dir=\"both\", edgetooltip=\"Not Response\", labeltooltip=\"Not Response\", arrowhead=\"normal\", arrowtail=\"dot\", style=\"dashed\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		case Not_Succession:
			return "[dir=\"both\", edgetooltip=\"Not Succession\", labeltooltip=\"Not Succession\", arrowhead=\"dotnormal\", arrowtail=\"dot\", style=\"dashed\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		default:
			throw new NoSuchElementException("Style not defined for the template in: " + binaryConstraint);
		}
	}
}
