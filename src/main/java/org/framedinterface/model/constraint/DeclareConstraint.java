package org.framedinterface.model.constraint;

import org.framedinterface.utils.enums.DeclareTemplate;

/**
 * A Declare constraint as written in a model file: its template, its activation and target activities, and their data and time
 * conditions.
 * <p>
 * This class isn't used anywhere in FrAIm (nor is the rest of its package). The Declare constraints FrAIm works with are
 * {@link org.framedinterface.model.DeclareConstraint}, which has the same name and fields, but takes the constructor arguments in a
 * different order and also replays the constraint. This class has been unchanged since the project's first commit.
 */
public class DeclareConstraint {
	private String constraintString;
	private DeclareTemplate template;
	private String activationActivity;
	private String activationCondition;
	private String targetActivity;
	private String targetCondition;
	private String timeCondition;
	
	/**
	 * Creates a constraint. Note the argument order, which differs from
	 * {@link org.framedinterface.model.DeclareConstraint}'s: each activity is followed by its condition.
	 *
	 * @param constraintString the constraint's line in the model file
	 * @param template the constraint's template
	 * @param activationActivity the activation activity
	 * @param activationCondition the activation activity's data condition
	 * @param targetActivity the target activity
	 * @param targetCondition the target activity's data condition
	 * @param timeCondition the constraint's time condition
	 */
	public DeclareConstraint(String constraintString, DeclareTemplate template, String activationActivity,
			String activationCondition, String targetActivity, String targetCondition, String timeCondition) {
		super();
		this.constraintString = constraintString;
		this.template = template;
		this.activationActivity = activationActivity;
		this.activationCondition = activationCondition;
		this.targetActivity = targetActivity;
		this.targetCondition = targetCondition;
		this.timeCondition = timeCondition;
	}

	/**
	 * Returns the constraint's line in the model file.
	 *
	 * @return the constraint line
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
	 * Returns the activation activity.
	 *
	 * @return the activation activity
	 */
	public String getActivationActivity() {
		return activationActivity;
	}

	/**
	 * Returns the activation activity's data condition.
	 *
	 * @return the activation condition
	 */
	public String getActivationCondition() {
		return activationCondition;
	}

	/**
	 * Returns the target activity.
	 *
	 * @return the target activity
	 */
	public String getTargetActivity() {
		return targetActivity;
	}

	/**
	 * Returns the target activity's data condition.
	 *
	 * @return the target condition
	 */
	public String getTargetCondition() {
		return targetCondition;
	}

	/**
	 * Returns the constraint's time condition.
	 *
	 * @return the time condition
	 */
	public String getTimeCondition() {
		return timeCondition;
	}

	/**
	 * Returns a description of the constraint for debugging, listing all its fields.
	 *
	 * @return the description
	 */
	@Override
	public String toString() {
		return "DeclareConstraint [constraintString=" + constraintString + ", template=" + template
				+ ", activationActivity=" + activationActivity + ", activationCondition=" + activationCondition
				+ ", targetActivity=" + targetActivity + ", targetCondition=" + targetCondition + ", timeCondition="
				+ timeCondition + "]";
	}
}
