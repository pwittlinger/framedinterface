package org.framedinterface.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The trace the pages currently work with, shared by all of them through a single instance (see {@link #getInstance()}): the
 * manually-built prefix, the attribute values attached to its events, and the plan (continuation) found for it, if any. While a plan is
 * present, the plan is the trace replayed on the models (see {@link #getActiveTrace()}), and the pages don't let the user edit the prefix.
 * <p>
 * The Data-Aware page keeps several prefixes, of which only the selected one is held here; it swaps them in and out with the setters.
 * Lists and maps are handed out and taken over as they are, not copied, so changes to them go straight into the session. Meant to be used
 * from the JavaFX application thread only.
 */
public class PlannerSession {

	private static final PlannerSession INSTANCE = new PlannerSession();

	private boolean planPresent;
	private boolean displayViolations;
	private List<String> currentPrefix = new ArrayList<String>();
	private List<String> currentPlan = new ArrayList<String>();
	private Map<Integer, Map<String, String>> prefixAttributeValues = new HashMap<Integer, Map<String, String>>(); //Keyed by the (1-based) event number in currentPrefix

	/**
	 * Only used for the single instance, {@link #INSTANCE}.
	 */
	private PlannerSession() {
	}

	/**
	 * Returns the session shared by all pages.
	 *
	 * @return the session
	 */
	public static PlannerSession getInstance() {
		return INSTANCE;
	}

	/**
	 * Tells whether a plan is present for the current prefix, in which case the plan is the active trace.
	 *
	 * @return {@code true} if a plan is present
	 */
	public boolean isPlanPresent() {
		return planPresent;
	}

	/**
	 * Sets whether a plan is present for the current prefix.
	 *
	 * @param planPresent {@code true} to make the current plan the active trace, {@code false} to make it the prefix
	 */
	public void setPlanPresent(boolean planPresent) {
		this.planPresent = planPresent;
	}

	/**
	 * Tells whether the visualizations show past violations (the "Show Past Violations" checkbox).
	 *
	 * @return {@code true} if past violations are shown
	 */
	public boolean isDisplayViolations() {
		return displayViolations;
	}

	/**
	 * Sets whether the visualizations show past violations.
	 *
	 * @param displayViolations {@code true} to show past violations
	 */
	public void setDisplayViolations(boolean displayViolations) {
		this.displayViolations = displayViolations;
	}

	/**
	 * Returns the current, manually-built prefix.
	 *
	 * @return the activities of the prefix's events, as the live list
	 */
	public List<String> getCurrentPrefix() {
		return currentPrefix;
	}

	/**
	 * Sets the current prefix, e.g. when the Data-Aware page switches to another prefix.
	 *
	 * @param currentPrefix the activities of the prefix's events; the list is used as is
	 */
	public void setCurrentPrefix(List<String> currentPrefix) {
		this.currentPrefix = currentPrefix;
	}

	/**
	 * Returns the plan found for the current prefix.
	 *
	 * @return one {@code action;activity} step per entry, e.g. {@code sync;ActivityA}, as the live list; empty if there's none
	 */
	public List<String> getCurrentPlan() {
		return currentPlan;
	}

	/**
	 * Sets the plan found for the current prefix. Whether it's the active trace is set with {@link #setPlanPresent(boolean)}.
	 *
	 * @param currentPlan one {@code action;activity} step per entry; the list is used as is
	 */
	public void setCurrentPlan(List<String> currentPlan) {
		this.currentPlan = currentPlan;
	}

	/**
	 * Returns the trace that should currently be used to evaluate the models' monitoring states: the plan if one is present, otherwise
	 * the manually-built prefix.
	 *
	 * @return the plan or the prefix, as the live list
	 */
	public List<String> getActiveTrace() {
		return planPresent ? currentPlan : currentPrefix;
	}

	/**
	 * Returns the attribute values manually attached to the prefix event at the given position. For an event without any, an empty map
	 * is created and kept, so values can be added to the returned map directly.
	 *
	 * @param eventNumber the event's position in the prefix, starting at 1
	 * @return attribute name to value, as the live map
	 */
	public Map<String, String> getPrefixAttributeValues(int eventNumber) {
		return prefixAttributeValues.computeIfAbsent(eventNumber, n -> new LinkedHashMap<String, String>());
	}

	/**
	 * Removes the attribute values of all prefix events.
	 */
	public void clearPrefixAttributeValues() {
		prefixAttributeValues.clear();
	}

	/**
	 * Returns the full attribute-values map backing {@link #getPrefixAttributeValues(int)}, exposed so callers can swap it out wholesale
	 * (e.g. the Data-Aware page, when switching between prefixes). {@link #removePrefixEvent(int)} replaces it with a new map, so it has to
	 * be fetched again after that.
	 *
	 * @return each prefix event's position (starting at 1) to its attribute values, as the live map
	 */
	public Map<Integer, Map<String, String>> getPrefixAttributeValuesMap() {
		return prefixAttributeValues;
	}

	/**
	 * Sets the attribute values of all prefix events, e.g. when the Data-Aware page switches to another prefix.
	 *
	 * @param prefixAttributeValues each prefix event's position (starting at 1) to its attribute values; the map is used as is
	 */
	public void setPrefixAttributeValuesMap(Map<Integer, Map<String, String>> prefixAttributeValues) {
		this.prefixAttributeValues = prefixAttributeValues;
	}

	/**
	 * Removes the prefix event at the given position, re-indexing the attribute values so they stay attached to their (now renumbered)
	 * events. The attribute-values map is replaced by a new one (see {@link #getPrefixAttributeValuesMap()}), while the prefix list is
	 * changed in place. Does nothing if there's no event at that position.
	 *
	 * @param eventNumber the event's position in the prefix, starting at 1
	 */
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
