package org.framedinterface.model;

import java.util.ArrayList;
import java.util.List;

public class PlannerSession {

	private static final PlannerSession INSTANCE = new PlannerSession();

	private boolean planPresent;
	private boolean displayViolations;
	private List<String> currentPrefix = new ArrayList<String>();
	private List<String> currentPlan = new ArrayList<String>();

	private PlannerSession() {
	}

	public static PlannerSession getInstance() {
		return INSTANCE;
	}

	public boolean isPlanPresent() {
		return planPresent;
	}

	public void setPlanPresent(boolean planPresent) {
		this.planPresent = planPresent;
	}

	public boolean isDisplayViolations() {
		return displayViolations;
	}

	public void setDisplayViolations(boolean displayViolations) {
		this.displayViolations = displayViolations;
	}

	public List<String> getCurrentPrefix() {
		return currentPrefix;
	}

	public void setCurrentPrefix(List<String> currentPrefix) {
		this.currentPrefix = currentPrefix;
	}

	public List<String> getCurrentPlan() {
		return currentPlan;
	}

	public void setCurrentPlan(List<String> currentPlan) {
		this.currentPlan = currentPlan;
	}

	// The trace that should currently be used to evaluate a model's monitoring states:
	// the executed plan if one is present, otherwise the manually-built prefix.
	public List<String> getActiveTrace() {
		return planPresent ? currentPlan : currentPrefix;
	}

}
