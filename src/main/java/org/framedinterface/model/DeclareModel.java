package org.framedinterface.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.BidiMap;

import org.framedinterface.utils.GraphGenerator;
import org.framedinterface.utils.enums.MonitoringState;


/**
 * A Declare model loaded from a {@code .decl} file: its activities, its constraints, the attributes bound to its activities and their
 * declared domains. Replaying a trace on the model (see {@link #updateMonitoringStates(List, boolean)} and
 * {@link #updateMonitoringStatesWithData(List, List, boolean)}) records each constraint's monitoring state at each position of the trace,
 * which the visualization and the event lists then show.
 * <p>
 * Activity and attribute names are lowercased; {@link #getActivityNameAsDefined(String)} gives an activity's spelling in the model file.
 * Models are created by {@link org.framedinterface.utils.ModelUtils#loadDeclareModel(java.nio.file.Path, String, String)}.
 */
public class DeclareModel extends AbstractModel  {
	
	private LinkedHashSet<DeclareConstraint> declareConstraints;
	private Map<String, List<DeclareConstraint>> activityToUnaryMap;
	private Map<String, LinkedHashSet<String>> activityToAttributesMap; //Bindings are only present in Declare models
	private Map<String, AttributeDomain> attributeDomains; //Attribute name (lowercased) -> its declared value range/set
	private List<Map<DeclareConstraint, MonitoringState>> monitoringStates; //First index is the initial state and last index is the final state
	private Map<String, DeclareConstraint> plannerConstraintNames = new HashMap<String, DeclareConstraint>(); //Lowercased PDDL constraint object name (Data-Aware planner) -> constraint; see setPlannerConstraintNames

	/**
	 * The action of a Data-Aware plan step that resets a violated constraint instead of executing an activity. Such a step is
	 * {@code reset constraint;<PDDL constraint name>}.
	 */
	public static final String RESET_CONSTRAINT_ACTION = "reset constraint";

	/**
	 * Creates a Declare model; normally done by
	 * {@link org.framedinterface.utils.ModelUtils#loadDeclareModel(java.nio.file.Path, String, String)}.
	 *
	 * @param modelId the model's id, unique among the loaded models
	 * @param modelName the model's name (its file name)
	 * @param activities the model's activities, lowercased
	 * @param activityToEncodingMap each activity's encoding in the visualization, e.g. {@code n0}
	 * @param declareConstraints the model's constraints, in file order
	 * @param activityToUnaryMap for each activity, its unary constraints (e.g. Existence or Init), which are shown on the activity's node
	 * @param activityToAttributesMap for each activity, the names of the attributes bound to it, lowercased
	 * @param attributeDomains for each attribute (lowercased), its declared value range or set
	 */
	public DeclareModel(String modelId, String modelName, LinkedHashSet<String> activities, BidiMap<String, String> activityToEncodingMap, LinkedHashSet<DeclareConstraint> declareConstraints, Map<String, List<DeclareConstraint>> activityToUnaryMap, Map<String, LinkedHashSet<String>> activityToAttributesMap, Map<String, AttributeDomain> attributeDomains) {
		super(modelId, modelName, activities, activityToEncodingMap, ModelType.DECLARE);
		this.declareConstraints = declareConstraints;
		this.activityToUnaryMap = activityToUnaryMap;
		this.activityToAttributesMap = activityToAttributesMap;
		this.attributeDomains = attributeDomains;
	}

	/**
	 * Returns the attributes bound to the given activity, by the model's "bind" lines or by its constraints' data conditions.
	 *
	 * @param activity the activity, lowercased
	 * @return the attribute names, lowercased; empty if none are bound
	 */
	public LinkedHashSet<String> getAttributesForActivity(String activity) {
		return activityToAttributesMap.getOrDefault(activity, new LinkedHashSet<String>());
	}

	/**
	 * Returns the declared value range or set of each attribute.
	 *
	 * @return attribute name (lowercased) to its domain
	 */
	public Map<String, AttributeDomain> getAttributeDomains() {
		return attributeDomains;
	}

	/**
	 * Returns the model's constraints.
	 *
	 * @return the constraints, in file order
	 */
	public LinkedHashSet<DeclareConstraint> getDeclareConstraints() {
		return declareConstraints;
	}

	/**
	 * Sets the names the Data-Aware page's PDDL generator gives this model's constraints, so that a plan's reset steps can be matched to
	 * them. The names are assigned by the caller, since the PDDL generator numbers duplicate names across all Declare models it is given
	 * together.
	 *
	 * @param plannerConstraintNames lowercased PDDL constraint name to constraint
	 */
	public void setPlannerConstraintNames(Map<String, DeclareConstraint> plannerConstraintNames) {
		this.plannerConstraintNames = plannerConstraintNames;
	}

	/**
	 * Finds the constraint the PDDL generator gave the given name (see {@link #setPlannerConstraintNames(Map)}).
	 *
	 * @param plannerConstraintName the name, in any casing; may be {@code null}
	 * @return the constraint, or {@code null} if none of this model's constraints has that name
	 */
	public DeclareConstraint getConstraintByPlannerName(String plannerConstraintName) {
		return plannerConstraintName == null ? null : plannerConstraintNames.get(plannerConstraintName.toLowerCase());
	}

	/**
	 * Tells whether any constraint of this model declares a time condition, e.g. {@code ActivityP,0,100,h/ActivityG,2,5,h}. The Data-Aware
	 * page uses it to decide whether prefix events need a "timestamp" attribute.
	 *
	 * @return {@code true} if some constraint has a time condition
	 */
	public boolean hasTimeConditions() {
		for (DeclareConstraint declareConstraint : declareConstraints) {
			String timeCondition = declareConstraint.getTimeCondition();
			if (timeCondition != null && !timeCondition.isBlank()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the constraints that the last replayed trace ends violated: those in {@link MonitoringState#VIOL} in its final monitoring
	 * states, in which possibly violated already counts as violated.
	 *
	 * @return the violated constraints; none if no trace was replayed
	 */
	public List<DeclareConstraint> getViolatedConstraintsAtTraceEnd() {
		List<DeclareConstraint> violatedConstraints = new ArrayList<DeclareConstraint>();
		if (monitoringStates != null && !monitoringStates.isEmpty()) {
			monitoringStates.get(monitoringStates.size() - 1).forEach((constraint, monitoringState) -> {
				if (monitoringState == MonitoringState.VIOL) {
					violatedConstraints.add(constraint);
				}
			});
		}
		return violatedConstraints;
	}

	/**
	 * Counts how many constraints are in each monitoring state at the given position of the last replayed trace.
	 *
	 * @param activityIndex the position: 0 for the initial states, then one per trace entry, and the last one for the final states
	 * @return the number of constraints in {@link MonitoringState#SAT}, {@link MonitoringState#POSS_SAT},
	 *         {@link MonitoringState#POSS_VIOL} and {@link MonitoringState#VIOL}; all 0 if no trace was replayed or the position is out
	 *         of range
	 */
	public Map<MonitoringState, Integer> getMonitoringStateCounts(int activityIndex) {
		Map<MonitoringState, Integer> monitoringStateCounts = new HashMap<MonitoringState, Integer>();
		monitoringStateCounts.put(MonitoringState.SAT, 0);
		monitoringStateCounts.put(MonitoringState.POSS_SAT, 0);
		monitoringStateCounts.put(MonitoringState.POSS_VIOL, 0);
		monitoringStateCounts.put(MonitoringState.VIOL, 0);
		if (monitoringStates == null || activityIndex < 0 || activityIndex >= monitoringStates.size()) {
			return monitoringStateCounts; //Not replayed (yet), or an index into a different trace
		}

		for (MonitoringState monitoringState : monitoringStates.get(activityIndex).values()) {
			monitoringStateCounts.put(monitoringState, monitoringStateCounts.get(monitoringState) + 1);
		}
		
		return monitoringStateCounts;
	}

	/**
	 * Replays the given trace on the model, ignoring data conditions (as on the Data-Agnostic page), and records each constraint's
	 * monitoring state before the first entry, after each entry and at the trace's end. At the end, possibly satisfied counts as satisfied
	 * and possibly violated as violated.
	 * <p>
	 * An activity that starts with "reset" (except {@link PnModel#RESET_PETRINET_ACTIVITY}) is taken as a reset step of the Data-Agnostic
	 * planner: the constraints whose template name and activities its name contains are reset to their initial state, and the step is then
	 * executed like any other activity. An activity named exactly "reset" makes this throw a {@link StringIndexOutOfBoundsException}.
	 *
	 * @param activities the trace: activities, or plan steps {@code action;activity}
	 * @param displayViolations unused by Declare models
	 */
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

	/**
	 * Data-Aware page only: replays the given trace like {@link #updateMonitoringStates(List, boolean)}, but each constraint's activation
	 * and target data conditions are checked against the attribute values attached to the occurring event before it counts as an
	 * activation or target occurrence. The Data-Agnostic page intentionally ignores data conditions and keeps using
	 * {@link #updateMonitoringStates(List, boolean)}.
	 * <p>
	 * A Data-Aware plan's reset step ({@code reset constraint;<PDDL constraint name>}, see {@link #RESET_CONSTRAINT_ACTION}) only returns
	 * the named constraint to its initial state, and executes no activity, which would otherwise advance e.g. chain constraints; it still
	 * gets a position of its own. Activities that start with "reset" are otherwise handled as in {@link #updateMonitoringStates(List, boolean)}.
	 *
	 * @param activities the trace: activities, or plan steps {@code action;activity}
	 * @param attributeValuesPerEvent the attribute values of each entry, parallel to {@code activities}
	 *        ({@code attributeValuesPerEvent.get(i)} holds those of {@code activities.get(i)}); missing entries count as no values
	 * @param displayViolations unused by Declare models
	 */
	public void updateMonitoringStatesWithData(List<String> activities, List<Map<String, String>> attributeValuesPerEvent, boolean displayViolations) {
		Map<DeclareConstraint, MonitoringState> initialStates = new HashMap<DeclareConstraint, MonitoringState>();
		declareConstraints.forEach(declareConstraint -> initialStates.put(declareConstraint, declareConstraint.resetAutomaton()));
		monitoringStates = new ArrayList<Map<DeclareConstraint,MonitoringState>>();
		monitoringStates.add(initialStates);

		for (int i = 0; i < activities.size(); i++) {
			String[] planAction = activities.get(i).split(";");
			String activity;

			if (planAction.length == 2) {
				activity = planAction[1];
			}
			else{
				activity = planAction[0];
			}

			//Data-Aware plan reset: only the named constraint returns to its initial state, and no activity is executed (which would otherwise advance e.g. chain constraints)
			if (planAction.length == 2 && planAction[0].equals(RESET_CONSTRAINT_ACTION)) {
				DeclareConstraint violatedConstraint = getConstraintByPlannerName(activity);
				if (violatedConstraint != null) {
					violatedConstraint.resetAutomaton();
				}
				Map<DeclareConstraint, MonitoringState> monitoringState = new HashMap<DeclareConstraint, MonitoringState>();
				declareConstraints.forEach(declareConstraint -> monitoringState.put(declareConstraint, declareConstraint.getMonitoringState()));
				monitoringStates.add(monitoringState);
				continue;
			}

			//Check if the action is reset-petrinet
			if (activity.startsWith("reset") && !activity.equals("reset-petrinet")) {
				String resetAct = activity.substring(6);

				for (DeclareConstraint dc : declareConstraints) {
					String templateName_ = dc.getTemplate().getTemplateName().toLowerCase();
					templateName_ = templateName_.replace(" ", "_");
					String activ_;
					if (dc.getTemplate().getReverseActivationTarget()) {
						activ_ = dc.getTargetActivity();
					} else {activ_ = dc.getActivationActivity();}

					if (resetAct.contains(templateName_) && (resetAct.contains(activ_.toLowerCase()))) {
						String target_;
						if (dc.getTemplate().getReverseActivationTarget()) {
								target_ = dc.getActivationActivity();
						} else {target_ = dc.getTargetActivity();}

						if (!(target_.isBlank())&&(!resetAct.contains(target_.toLowerCase()))){
							continue;
						}
						dc.resetAutomaton();
					}
				}
			}

			Map<String, String> attributeValues = i < attributeValuesPerEvent.size() ? attributeValuesPerEvent.get(i) : Collections.emptyMap();
			Map<DeclareConstraint, MonitoringState> monitoringState = new HashMap<DeclareConstraint, MonitoringState>();
			declareConstraints.forEach(declareConstraint -> monitoringState.put(declareConstraint, declareConstraint.executeNextActivity(activity.toLowerCase(), attributeValues)));
			monitoringStates.add(monitoringState);
		}

		//Adding final monitoring states (constraint states when the trace terminates)
		Map<DeclareConstraint, MonitoringState> finalStates = new HashMap<DeclareConstraint, MonitoringState>();
		declareConstraints.forEach(declareConstraint -> {
			MonitoringState monitoringState = declareConstraint.getMonitoringState();
			if (monitoringState == MonitoringState.POSS_SAT) { monitoringState = MonitoringState.SAT;}
			else if (monitoringState == MonitoringState.POSS_VIOL) { monitoringState = MonitoringState.VIOL;}
			finalStates.put(declareConstraint, monitoringState);
		});
		monitoringStates.add(finalStates);
	}

	/**
	 * Returns the model's visualization at the given position of the last replayed trace, without data conditions (see
	 * {@link #getVisualisationString(int, boolean, boolean)}).
	 *
	 * @param activityIndex the position, see {@link #getMonitoringStateCounts(int)}
	 * @param displayViolations unused by Declare models
	 * @return the visualization, in Graphviz DOT
	 */
	@Override
	public String getVisualisationString(int activityIndex, boolean displayViolations) {
		return getVisualisationString(activityIndex, displayViolations, false);
	}

	/**
	 * Returns the model's visualization at the given position of the last replayed trace: a node per activity (showing its unary
	 * constraints) and an edge per binary constraint, each showing the constraints' monitoring states at that position. Data conditions
	 * (e.g. {@code [A.grade > 4]}) are only meaningful for Declare models, so this overload lives here rather than on
	 * {@link AbstractModel}.
	 *
	 * @param activityIndex the position, see {@link #getMonitoringStateCounts(int)}; a trace must have been replayed
	 * @param displayViolations unused by Declare models
	 * @param showDataConditions whether to show the constraints' data conditions
	 * @return the visualization, in Graphviz DOT, with {@code '} escaped for the visualization page's JavaScript
	 */
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

	/**
	 * Returns the automata of all constraints to their initial state. The monitoring states recorded by the last replay are kept until the
	 * next one.
	 */
	@Override
	public void resetModel(){

		for (DeclareConstraint dc : declareConstraints) {
			dc.resetAutomaton();
		}

	}
	
	

}
