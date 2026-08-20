package org.framedinterface.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlannerSession {

	private static final PlannerSession INSTANCE = new PlannerSession();

	private boolean planPresent;
	private boolean displayViolations;
	private List<String> currentPrefix = new ArrayList<String>();
	private List<String> currentPlan = new ArrayList<String>();
	private Map<Integer, Map<String, String>> prefixAttributeValues = new HashMap<Integer, Map<String, String>>(); //Keyed by the (1-based) event number in currentPrefix

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

	//Attribute values (attribute name -> value) manually attached to the prefix event at the given (1-based) position
	public Map<String, String> getPrefixAttributeValues(int eventNumber) {
		return prefixAttributeValues.computeIfAbsent(eventNumber, n -> new LinkedHashMap<String, String>());
	}

	public void clearPrefixAttributeValues() {
		prefixAttributeValues.clear();
	}

	//Removes the prefix event at the given (1-based) position, re-indexing attribute values so they stay attached to their (now renumbered) events
	public void removePrefixEvent(int eventNumber) {
		int index = eventNumber - 1;
		if (index < 0 || index >= currentPrefix.size()) {
			return;
		}
		currentPrefix.remove(index);

		Map<Integer, Map<String, String>> reindexed = new HashMap<Integer, Map<String, String>>();
		for (Map.Entry<Integer, Map<String, String>> entry : prefixAttributeValues.entrySet()) {
			int key = entry.getKey();
			if (key < eventNumber) {
				reindexed.put(key, entry.getValue());
			} else if (key > eventNumber) {
				reindexed.put(key - 1, entry.getValue());
			}
			//key == eventNumber is the deleted event's own attribute values, dropped
		}
		prefixAttributeValues = reindexed;
	}

}
