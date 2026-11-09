package org.framedinterface.model;

import java.util.Map;

import org.processmining.plugins.declareminer.ExecutableAutomaton;

import org.framedinterface.utils.AutomatonUtils;
import org.framedinterface.utils.DataConditionEvaluator;
import org.framedinterface.utils.enums.DeclareTemplate;
import org.framedinterface.utils.enums.MonitoringState;

/**
 * A constraint of a Declare model, read from its line in the model file, e.g.
 * {@code Response[ActivityG, ActivityP] |A.integer > 10 |T.integer < 10 | ActivityP,0,100,h/ActivityG,2,5,h}: its template, its
 * activation and target activities (lowercased), their data conditions, and its time condition.
 * <p>
 * Each constraint has its own automaton for replaying traces, built from its template's LTL formula by {@link AutomatonUtils}. Events
 * are fed to it as the letter "A" (the activation), "B" (the target) or "Z" (any other activity). For templates that reverse activation
 * and target (e.g. Precedence), the activation is "B" and the target "A".
 * <p>
 * Two constraints are equal if they have the same template and activities (see {@link #equals(Object)}).
 */
public class DeclareConstraint {
	private String constraintString;
	private DeclareTemplate template;
	private String activationActivity;
	private String targetActivity;
	private String activationCondition;
	private String targetCondition;
	private String timeCondition;

	private ExecutableAutomaton constraintAutomaton; //Attaching the automata directly to constraint object, might be more convenient in this case

	/**
	 * Creates a constraint and its automaton.
	 *
	 * @param constraintString the constraint's line in the model file
	 * @param template the constraint's template; must not be {@code null}
	 * @param activationActivity the activation activity, lowercased
	 * @param targetActivity the target activity, lowercased; empty for a unary constraint
	 * @param activationCondition the activation's data condition, e.g. {@code A.integer > 10}; empty for none
	 * @param targetCondition the target's data condition, e.g. {@code T.integer < 10}; empty for none
	 * @param timeCondition the time condition, e.g. {@code ActivityP,0,100,h/ActivityG,2,5,h}; empty for none
	 */
	public DeclareConstraint(String constraintString, DeclareTemplate template, String activationActivity, String targetActivity,
			String activationCondition, String targetCondition, String timeCondition) {
		super();
		this.constraintString = constraintString;
		this.template = template;
		this.activationActivity = activationActivity;
		this.targetActivity = targetActivity;
		this.activationCondition = activationCondition;
		this.targetCondition = targetCondition;
		this.timeCondition = timeCondition;

		constraintAutomaton = AutomatonUtils.createConstraintAutomaton(this);
	}

	/**
	 * Returns the constraint's line in the model file.
	 *
	 * @return the constraint line, including its conditions
	 */
	public String getConstraintString() {
		return constraintString;
	}

	/**
	 * Returns the constraint's template.
	 *
	 * @return the template
	 */
	public DeclareTemplate getTemplate() {
		return template;
	}

	/**
	 * Returns the activation activity, i.e. the one whose occurrence activates the constraint. For templates that reverse activation and
	 * target (e.g. Precedence), it's the second activity in the model file.
	 *
	 * @return the activation activity, lowercased
	 */
	public String getActivationActivity() {
		return activationActivity;
	}

	/**
	 * Returns the target activity.
	 *
	 * @return the target activity, lowercased; empty for a unary constraint
	 */
	public String getTargetActivity() {
		return targetActivity;
	}

	/**
	 * Returns the activation's data condition.
	 *
	 * @return the condition, e.g. {@code A.integer > 10}; empty for none
	 */
	public String getActivationCondition() {
		return activationCondition;
	}

	/**
	 * Returns the target's data condition.
	 *
	 * @return the condition, e.g. {@code T.integer < 10}; empty for none
	 */
	public String getTargetCondition() {
		return targetCondition;
	}

	/**
	 * Returns the constraint's time condition.
	 *
	 * @return the condition, e.g. {@code ActivityP,0,100,h/ActivityG,2,5,h}; empty for none
	 */
	public String getTimeCondition() {
		return timeCondition;
	}


	//Methods for trace replay
	/**
	 * Returns the automaton to its initial state, before any event of the trace.
	 *
	 * @return the monitoring state in the initial state
	 */
	public MonitoringState resetAutomaton() {
		constraintAutomaton.ini();
		return AutomatonUtils.getMonitoringState(constraintAutomaton);
	}
	
	/**
	 * Returns the monitoring state of the automaton's current state.
	 *
	 * @return {@link MonitoringState#SAT}, {@link MonitoringState#POSS_SAT}, {@link MonitoringState#POSS_VIOL} or
	 *         {@link MonitoringState#VIOL}
	 */
	public MonitoringState getMonitoringState() {
		return AutomatonUtils.getMonitoringState(constraintAutomaton);
	}
	
	/**
	 * Feeds the trace's next event to the automaton, ignoring data conditions: the activation activity counts as the activation, the
	 * target activity as the target, and any other activity as an arbitrary event, which still matters for e.g. chain constraints. If the
	 * activation and target are the same activity, each occurrence only counts as the activation.
	 *
	 * @param activityName the event's activity, lowercased
	 * @return the monitoring state after the event
	 */
	public MonitoringState executeNextActivity(String activityName) {
		constraintAutomaton.next(getAutomataLabel(activityName));
		return AutomatonUtils.getMonitoringState(constraintAutomaton);
	}

	/**
	 * Data-Aware page only: feeds the trace's next event to the automaton like {@link #executeNextActivity(String)}, but an occurrence
	 * only counts as the activation or target if its attribute values also satisfy the corresponding data condition. Otherwise it's
	 * treated as an arbitrary (data-irrelevant) event, same as "Z".
	 *
	 * @param activityName the event's activity, lowercased
	 * @param attributeValues the event's attribute values (attribute name to value)
	 * @return the monitoring state after the event
	 */
	public MonitoringState executeNextActivity(String activityName, Map<String, String> attributeValues) {
		constraintAutomaton.next(getAutomataLabel(activityName, attributeValues));
		return AutomatonUtils.getMonitoringState(constraintAutomaton);
	}

	/**
	 * Returns the automaton letter for the given activity, ignoring data conditions.
	 *
	 * @param activityName the activity, lowercased
	 * @return "A" for the activation and "B" for the target (swapped for templates that reverse them), "Z" for any other activity
	 */
	private String getAutomataLabel(String activityName) {
		if (activityName.equals(activationActivity)) {
			return template.getReverseActivationTarget() ? "B" : "A";
		} else if (activityName.equals(targetActivity)) {
			return template.getReverseActivationTarget() ? "A" : "B";
		} else {
			return "Z"; //Arbitrary activities are still relevant for chain constraints
		}
	}

	/**
	 * Returns the automaton letter for the given event, taking data conditions into account.
	 *
	 * @param activityName the event's activity, lowercased
	 * @param attributeValues the event's attribute values
	 * @return as {@link #getAutomataLabel(String)}, but "Z" for an activation or target whose data condition isn't satisfied
	 */
	private String getAutomataLabel(String activityName, Map<String, String> attributeValues) {
		if (activityName.equals(activationActivity) && DataConditionEvaluator.evaluate(activationCondition, attributeValues)) {
			return template.getReverseActivationTarget() ? "B" : "A";
		} else if (activityName.equals(targetActivity) && DataConditionEvaluator.evaluate(targetCondition, attributeValues)) {
			return template.getReverseActivationTarget() ? "A" : "B";
		} else {
			return "Z"; //Arbitrary activities, and activities whose data condition wasn't satisfied, are still relevant for chain constraints
		}
	}
	
	/**
	 * Computes a hash code from the template and the activities, consistently with {@link #equals(Object)}.
	 *
	 * @return the hash code
	 */
	@Override
	public int hashCode() { //For uniqueness check in LinkedHashSet
		final int prime = 31;
		int result = 1;
		result = prime * result + ((activationActivity == null) ? 0 : activationActivity.hashCode());
		result = prime * result + ((targetActivity == null) ? 0 : targetActivity.hashCode());
		result = prime * result + ((template == null) ? 0 : template.hashCode());
		return result;
	}
	
	/**
	 * Tells whether the given object is a constraint with the same template and the same activation and target activities. Used to drop
	 * duplicate constraints when a model is loaded, as the model keeps its constraints in a {@code LinkedHashSet}. The data and time
	 * conditions aren't compared, so of two constraints that only differ in their conditions, only the first is kept.
	 *
	 * @param obj the object to compare with
	 * @return {@code true} if it's an equal constraint
	 */
	@Override
	public boolean equals(Object obj) { //For uniqueness check in LinkedHashSet
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DeclareConstraint other = (DeclareConstraint) obj;
		if (activationActivity == null) {
			if (other.activationActivity != null)
				return false;
		} else if (!activationActivity.equals(other.activationActivity))
			return false;
		if (targetActivity == null) {
			if (other.targetActivity != null)
				return false;
		} else if (!targetActivity.equals(other.targetActivity))
			return false;
		if (template != other.template)
			return false;
		return true;
	}

	/**
	 * Returns a description of the constraint for debugging: its line in the model file, its template and its activities.
	 *
	 * @return the description
	 */
	@Override
	public String toString() {
		return "DeclareConstraint [constraintString=" + constraintString + ", template=" + template
				+ ", activationActivity=" + activationActivity + ", targetActivity=" + targetActivity + "]";
	}

}
