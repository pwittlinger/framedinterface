package org.framedinterface.event;

import java.util.Map;

import org.framedinterface.utils.enums.MonitoringState;

/**
 * The data of one row of the Prefix and Continuation lists (shown by {@link EventCell}): an event of the trace, or the trace's start or
 * end. Besides the event's number and activity, it holds the plan action that replayed it, the attribute values attached to it, its
 * position in the replayed trace (the timeline), and how many constraints of the visualized Declare model are in each monitoring state at
 * that position.
 */
public class EventData {
	private static final Map<MonitoringState, Integer> EMPTY_MONITORING_STATE_COUNTS = Map.of(
			MonitoringState.SAT, 0,
			MonitoringState.POSS_SAT, 0,
			MonitoringState.POSS_VIOL, 0,
			MonitoringState.VIOL, 0);

	private int eventNumber; //Event number in the trace
	private String activityName;
	private Map<MonitoringState, Integer> declMonitoringStateCounts = EMPTY_MONITORING_STATE_COUNTS;
	private String plannerAction;
	private Map<String, String> attributeValues; //Attribute name -> value, manually attached by the user; only populated for editable (prefix, no plan present) events
	private int timelineIndex; //Position of this event in the replayed (timeline) trace; differs from eventNumber when the list isn't the replayed trace itself (e.g. prefix events while a plan is replayed)

	private boolean isStart;
	private boolean isEnd;

	/**
	 * Creates an event whose timeline position is its event number.
	 *
	 * @param eventNumber the event's number in its list's trace, the trace start being 0
	 * @param activityName the event's activity, or {@code null} for the trace's start or end
	 */
	public EventData(int eventNumber, String activityName) {
		this.eventNumber = eventNumber;
		this.activityName = activityName;
		this.timelineIndex = eventNumber;
	}

	/**
	 * Creates an event replayed by a plan step, whose timeline position is its event number.
	 *
	 * @param eventNumber the event's number in the plan, the trace start being 0
	 * @param activityName the event's activity, or what the step is shown as (e.g. the constraint a reset step resets)
	 * @param planAction the plan action, or a description of the plan step, shown below the activity, e.g.
	 *        {@code add [integer=55] @ 2023-08-07T12:00:00}
	 */
	public EventData(int eventNumber, String activityName, String planAction) {
		this(eventNumber, activityName);
		this.plannerAction = planAction;
	}

	/**
	 * Returns the event's position in the replayed trace (the timeline). It differs from the event number when the list isn't the
	 * replayed trace itself, e.g. for prefix events while their plan is replayed.
	 *
	 * @return the timeline position, or -1 if the event has no position of its own in the replayed trace
	 */
	public int getTimelineIndex() {
		return timelineIndex;
	}

	/**
	 * Sets the event's position in the replayed trace (the timeline).
	 *
	 * @param timelineIndex the timeline position, or -1 if the event has no position of its own in the replayed trace
	 */
	public void setTimelineIndex(int timelineIndex) {
		this.timelineIndex = timelineIndex;
	}

	/**
	 * Sets the plan action shown below the activity, e.g. the action of the plan step that replayed a prefix event.
	 *
	 * @param planAction the plan action (e.g. {@code sync}), or a description of the plan step; {@code null} for none
	 */
	public void setPlanAction(String planAction) {
		this.plannerAction = planAction;
	}
	
	//This might be pad practice for instantiating a class
	/**
	 * Creates the row for the start of the trace, with event number and timeline position 0.
	 *
	 * @return the trace start
	 */
	public static EventData createStartEvent() {
		EventData startEvent = new EventData(0, null);
		startEvent.isStart = true;
		return startEvent;
	}
	/**
	 * Creates the row for the end of the trace.
	 *
	 * @param eventNumber the end's event number (and timeline position), one more than the trace's last event
	 * @return the trace end
	 */
	public static EventData createEndEvent(int eventNumber) {
		EventData startEvent = new EventData(eventNumber, null);
		startEvent.isEnd = true;
		return startEvent;
	}

	/**
	 * Returns the event's number in its list's trace.
	 *
	 * @return the event number, 0 for the trace start
	 */
	public int getEventNumber() {
		return eventNumber;
	}
	
	/**
	 * Returns the event's activity.
	 *
	 * @return the activity, or {@code null} for the trace's start or end
	 */
	public String getActivityName() {
		return activityName;
	}

	/**
	 * Returns the plan action shown below the activity.
	 *
	 * @return the plan action, or a description of the plan step; {@code null} for none
	 */
	public String getPlanAction() {
		return this.plannerAction;
	}

	/**
	 * Sets the attribute values attached to the event. The map is used as is, so the attribute editor's changes go straight into it.
	 *
	 * @param attributeValues attribute name to value; {@code null} if the event's attribute values can't be edited (e.g. it isn't part of
	 *        an editable prefix)
	 */
	public void setAttributeValues(Map<String, String> attributeValues) {
		this.attributeValues = attributeValues;
	}

	/**
	 * Returns the attribute values attached to the event.
	 *
	 * @return attribute name to value; {@code null} if the event's attribute values can't be edited
	 */
	public Map<String, String> getAttributeValues() {
		return attributeValues;
	}
	
	/**
	 * Sets how many constraints of the visualized Declare model are in each monitoring state at the event's timeline position.
	 *
	 * @param declMonitoringStateCounts the number of constraints per state; must contain {@link MonitoringState#SAT},
	 *        {@link MonitoringState#POSS_SAT}, {@link MonitoringState#POSS_VIOL} and {@link MonitoringState#VIOL}
	 */
	public void setDeclMonitoringStateCounts(Map<MonitoringState, Integer> declMonitoringStateCounts) {
		this.declMonitoringStateCounts = declMonitoringStateCounts;
	}
	/**
	 * Returns how many constraints of the visualized Declare model are in each monitoring state at the event's timeline position.
	 *
	 * @return the number of constraints per state; all 0 until set
	 */
	public Map<MonitoringState, Integer> getDeclMonitoringStateCounts() {
		return declMonitoringStateCounts;
	}
	
	/**
	 * Tells whether this row is the start of the trace.
	 *
	 * @return {@code true} for the trace start
	 */
	public boolean isStart() {
		return isStart;
	}
	/**
	 * Tells whether this row is the end of the trace.
	 *
	 * @return {@code true} for the trace end
	 */
	public boolean isEnd() {
		return isEnd;
	}

	/**
	 * Returns a description of the event for debugging, e.g. {@code EventData [eventNumber=1, activityName=ActivityA, ...]}.
	 *
	 * @return the description
	 */
	@Override
	public String toString() {
		return "EventData [eventNumber=" + eventNumber + ", activityName=" + activityName
				+ ", declMonitoringStateCounts=" + declMonitoringStateCounts + ", isStart=" + isStart + ", isEnd="
				+ isEnd + "]";
	}	
}
