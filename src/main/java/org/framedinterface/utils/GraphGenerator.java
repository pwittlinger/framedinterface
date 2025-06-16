package org.framedinterface.utils;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.collections4.BidiMap;

import org.framedinterface.model.DeclareConstraint;
import org.framedinterface.utils.enums.MonitoringState;

public class GraphGenerator {

	private GraphGenerator() {
		//Private constructor to avoid unnecessary instantiation of the class
	}


	//Used for monitoring and editor; creates a node strings with html table if the node has unary constraints (otherwise normal node strings are used)
	public static String buildDeclNodeString(String nodeId, String activityName, Map<DeclareConstraint, MonitoringState> monitoringStates, List<DeclareConstraint> activityUnaryConstraints) {
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
				//Coloring based on the constraint status
				if (monitoringState == MonitoringState.CONFLICT) {
					sb.append("#ff9900\">").append(unaryConstraint.getTemplate().getTemplateName());
				} else if (monitoringState == MonitoringState.SAT) {
					sb.append("#66ccff\">").append(unaryConstraint.getTemplate().getTemplateName());
				} else if (monitoringState == MonitoringState.VIOL) {
					sb.append("#d44942\">").append(unaryConstraint.getTemplate().getTemplateName());
				} else if (monitoringState == MonitoringState.POSS_VIOL) {
					sb.append("#ffd700\">").append(unaryConstraint.getTemplate().getTemplateName());
				} else if (monitoringState == MonitoringState.POSS_SAT) {
					sb.append("#79a888\">").append(unaryConstraint.getTemplate().getTemplateName());
				} else {
					sb.append("#000000\"><font color=\"white\">").append(unaryConstraint.getTemplate().getTemplateName()).append("</font>"); //Fallback for unknown monitoring states
				}
				sb.append("</td></tr>");
			}

			sb.append("<tr><td bgcolor=\"blue\"><font color=\"white\">");
			sb.append(activityName);
			sb.append("</font></td></tr></table>>]");

			return sb.toString();
		}
	}

	public static String buildDeclEdgeString(DeclareConstraint binaryConstraint, MonitoringState monitoringState, BidiMap<String, String> activityToEncodingMap) {
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

		sb.append(" ").append(getBinaryConstraintStyle(binaryConstraint, monitoringState, ""));
		return sb.toString();
	}


	private static String getBinaryConstraintStyle(DeclareConstraint binaryConstraint, MonitoringState monitoringState, String penwidth) {
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


		String label = binaryConstraint.getTemplate().getTemplateName();
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
