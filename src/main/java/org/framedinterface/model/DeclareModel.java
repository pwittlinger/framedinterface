package org.framedinterface.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.BidiMap;

import org.framedinterface.utils.GraphGenerator;
import org.framedinterface.utils.enums.MonitoringState;


public class DeclareModel extends AbstractModel  {
	
	private LinkedHashSet<DeclareConstraint> declareConstraints;
	private Map<String, List<DeclareConstraint>> activityToUnaryMap;
	private Map<String, LinkedHashSet<String>> activityToAttributesMap; //Bindings are only present in Declare models
	private Map<String, AttributeDomain> attributeDomains; //Attribute name (lowercased) -> its declared value range/set
	private List<Map<DeclareConstraint, MonitoringState>> monitoringStates; //First index is the initial state and last index is the final state

	public DeclareModel(String modelId, String modelName, LinkedHashSet<String> activities, BidiMap<String, String> activityToEncodingMap, LinkedHashSet<DeclareConstraint> declareConstraints, Map<String, List<DeclareConstraint>> activityToUnaryMap, Map<String, LinkedHashSet<String>> activityToAttributesMap, Map<String, AttributeDomain> attributeDomains) {
		super(modelId, modelName, activities, activityToEncodingMap, ModelType.DECLARE);
		this.declareConstraints = declareConstraints;
		this.activityToUnaryMap = activityToUnaryMap;
		this.activityToAttributesMap = activityToAttributesMap;
		this.attributeDomains = attributeDomains;
	}

	//Attribute names (lowercased) bound to the given activity (lowercased), or an empty set if none are bound
	public LinkedHashSet<String> getAttributesForActivity(String activity) {
		return activityToAttributesMap.getOrDefault(activity, new LinkedHashSet<String>());
	}

	public Map<String, AttributeDomain> getAttributeDomains() {
		return attributeDomains;
	}
	
	public Map<MonitoringState, Integer> getMonitoringStateCounts(int activityIndex) {
		Map<MonitoringState, Integer> monitoringStateCounts = new HashMap<MonitoringState, Integer>();
		monitoringStateCounts.put(MonitoringState.SAT, 0);
		monitoringStateCounts.put(MonitoringState.POSS_SAT, 0);
		monitoringStateCounts.put(MonitoringState.POSS_VIOL, 0);
		monitoringStateCounts.put(MonitoringState.VIOL, 0);
		
		for (MonitoringState monitoringState : monitoringStates.get(activityIndex).values()) {
			monitoringStateCounts.put(monitoringState, monitoringStateCounts.get(monitoringState) + 1);
		}
		
		return monitoringStateCounts;
	}

	@Override
	public void updateMonitoringStates(List<String> activities, boolean displayViolations) {
		//Resetting the automata and adding initial monitoring states (constraint states before any events occur)
		Map<DeclareConstraint, MonitoringState> initialStates = new HashMap<DeclareConstraint, MonitoringState>();
		declareConstraints.forEach(declareConstraint -> initialStates.put(declareConstraint, declareConstraint.resetAutomaton()));
		monitoringStates = new ArrayList<Map<DeclareConstraint,MonitoringState>>();
		monitoringStates.add(initialStates);
		
		for (String act : activities) {
			String[] planAction = act.split(";");
			String activity;
			
			
			if (planAction.length == 2) {
				activity = planAction[1];
			}
			else{
				activity = planAction[0];
			}

			//Check if the action is reset-petrinet
			if (activity.startsWith("reset") && !activity.equals("reset-petrinet")) {
				// Format for Plan output us absence_activitym
				
				//String[] resetAct = activity.split("_");
				String resetAct = activity.substring(6);
				//first remove "reset-": this part is present on all of them
				
				for (DeclareConstraint dc : declareConstraints) {

					// This causes an issue for no coexistence constraints, as the planner appends another _ and -
					// This turns it into not_co-existence_act1_act2
					String templateName_ = dc.getTemplate().getTemplateName().toLowerCase(); // not co-existence
					//
					templateName_ = templateName_.replace(" ", "_");// Making sure the planner output and the template name match up
					String activ_;
					if (dc.getTemplate().getReverseActivationTarget()) {
						activ_ = dc.getTargetActivity();
					} else {activ_ = dc.getActivationActivity();}
					
					//if ((templateName_.equals(resetAct[0].split("-")[1])) && (activ_.toLowerCase().equals(resetAct[1]))) {
					if (resetAct.contains(templateName_) && (resetAct.contains(activ_.toLowerCase()))) {
						String target_;
							if (dc.getTemplate().getReverseActivationTarget()) {
									target_ = dc.getActivationActivity();
							} else {target_ = dc.getTargetActivity();}

						if (!(target_.isBlank())&&(!resetAct.contains(target_.toLowerCase()))){
							continue;
						}
						/*if (resetAct.length>2) {
							String target_;
							if (dc.getTemplate().getReverseActivationTarget()) {
									target_ = dc.getActivationActivity();
							} else {target_ = dc.getTargetActivity();}
							if (!target_.toLowerCase().equals(resetAct[2])) {
								continue;
							}
						}
						*/
						dc.resetAutomaton();
					}
				}
			}

			Map<DeclareConstraint, MonitoringState> monitoringState = new HashMap<DeclareConstraint, MonitoringState>();
			declareConstraints.forEach(declareConstraint -> monitoringState.put(declareConstraint, declareConstraint.executeNextActivity(activity.toLowerCase())));
			monitoringStates.add(monitoringState);
			
			
		}
		
		//Adding final monitoring states  (constraint states when the trace terminates)
		Map<DeclareConstraint, MonitoringState> finalStates = new HashMap<DeclareConstraint, MonitoringState>();
		declareConstraints.forEach(declareConstraint -> {
			MonitoringState monitoringState = declareConstraint.getMonitoringState();
			if (monitoringState == MonitoringState.POSS_SAT) { monitoringState = MonitoringState.SAT;}
			else if (monitoringState == MonitoringState.POSS_VIOL) { monitoringState = MonitoringState.VIOL;}
			finalStates.put(declareConstraint, monitoringState);
		});
		monitoringStates.add(finalStates);
	}

	@Override
	public String getVisualisationString(int activityIndex, boolean displayViolations) {
		return getVisualisationString(activityIndex, displayViolations, false);
	}

	//Data conditions (e.g. "[A.grade > 4]") are only meaningful for Declare models, so this overload lives here rather than on AbstractModel
	public String getVisualisationString(int activityIndex, boolean displayViolations, boolean showDataConditions) {
		StringBuilder sb = new StringBuilder("digraph \"\" {");
		sb.append("id = \"graphRoot_" + getModelId() + "\"");
		sb.append("ranksep = \".6\"");
		sb.append("nodesep = \".5\"");
		sb.append("node [style=\"filled\", shape=box, fontsize=\"8\", fontname=\"Helvetica\"]");
		sb.append("edge [fontsize=\"8\", fontname=\"Helvetica\" arrowsize=\".8\"]");

		for (String activity : this.getActivities()) {
			sb.append(GraphGenerator.buildDeclNodeString(this.getActivityEncoding(activity), activity, monitoringStates.get(activityIndex), activityToUnaryMap.get(activity), showDataConditions));
		}

		for (DeclareConstraint declareConstraint : declareConstraints) {
			if (declareConstraint.getTemplate().getIsBinary()) {
				sb.append(GraphGenerator.buildDeclEdgeString(declareConstraint, monitoringStates.get(activityIndex).get(declareConstraint), this.getActivityToEncodingMap(), showDataConditions));
			}
		}

		sb.append("}");

		//System.out.println(sb.toString());
		return sb.toString().replace("'", "\\'"); //A crude fix to allow ' characters in activity names
	}

	@Override
	public void resetModel(){

		for (DeclareConstraint dc : declareConstraints) {
			dc.resetAutomaton();
		}

	}
	
	

}
