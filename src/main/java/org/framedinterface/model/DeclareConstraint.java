package org.framedinterface.model;

import org.processmining.plugins.declareminer.ExecutableAutomaton;

import org.framedinterface.utils.AutomatonUtils;
import org.framedinterface.utils.enums.DeclareTemplate;
import org.framedinterface.utils.enums.MonitoringState;

public class DeclareConstraint {
	private String constraintString;
	private DeclareTemplate template;
	private String activationActivity;
	private String targetActivity;
	
	private ExecutableAutomaton constraintAutomaton; //Attaching the automata directly to constraint object, might be more convenient in this case

	public DeclareConstraint(String constraintString, DeclareTemplate template, String activationActivity, String targetActivity) {
		super();
		this.constraintString = constraintString;
		this.template = template;
		this.activationActivity = activationActivity;
		this.targetActivity = targetActivity;
		
		constraintAutomaton = AutomatonUtils.createConstraintAutomaton(this);
	}

	public String getConstraintString() {
		return constraintString;
	}

	public DeclareTemplate getTemplate() {
		return template;
	}

	public String getActivationActivity() {
		return activationActivity;
	}

	public String getTargetActivity() {
		return targetActivity;
	}
	
	
	//Methods for trace replay
	public MonitoringState resetAutomaton() {
		constraintAutomaton.ini();
		return AutomatonUtils.getMonitoringState(constraintAutomaton);
	}
	
	public MonitoringState getMonitoringState() {
		return AutomatonUtils.getMonitoringState(constraintAutomaton);
	}
	
	public MonitoringState executeNextActivity(String activityName) {
		constraintAutomaton.next(getAutomataLabel(activityName));
		return AutomatonUtils.getMonitoringState(constraintAutomaton);
	}
	
	private String getAutomataLabel(String activityName) {
		if (activityName.equals(activationActivity)) {
			return template.getReverseActivationTarget() ? "B" : "A"; 
		} else if (activityName.equals(targetActivity)) {
			return template.getReverseActivationTarget() ? "A" : "B"; 
		} else {
			return "Z"; //Arbitrary activities are still relevant for chain constraints
		}
	}
	
	@Override
	public int hashCode() { //For uniqueness check in LinkedHashSet
		final int prime = 31;
		int result = 1;
		result = prime * result + ((activationActivity == null) ? 0 : activationActivity.hashCode());
		result = prime * result + ((targetActivity == null) ? 0 : targetActivity.hashCode());
		result = prime * result + ((template == null) ? 0 : template.hashCode());
		return result;
	}
	
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

	@Override
	public String toString() {
		return "DeclareConstraint [constraintString=" + constraintString + ", template=" + template
				+ ", activationActivity=" + activationActivity + ", targetActivity=" + targetActivity + "]";
	}

}
