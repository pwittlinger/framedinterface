package org.framedinterface.event;

import java.util.Map;

import org.framedinterface.utils.enums.MonitoringState;

public class EventData {
	private int eventNumber; //Event number in the trace
	private String activityName;
	private Map<MonitoringState, Integer> declMonitoringStateCounts;
	private String plannerAction;
	
	private boolean isStart;
	private boolean isEnd;
	
	public EventData(int eventNumber, String activityName) {
		this.eventNumber = eventNumber;
		this.activityName = activityName;
	}

	public EventData(int eventNumber, String activityName, String planAction) {
		this.eventNumber = eventNumber;
		this.activityName = activityName;
		this.plannerAction = planAction;
	}
	
	//This might be pad practice for instantiating a class
	public static EventData createStartEvent() {
		EventData startEvent = new EventData(0, null);
		startEvent.isStart = true;
		return startEvent;
	}
	public static EventData createEndEvent(int eventNumber) {
		EventData startEvent = new EventData(eventNumber, null);
		startEvent.isEnd = true;
		return startEvent;
	}

	public int getEventNumber() {
		return eventNumber;
	}
	
	public String getActivityName() {
		return activityName;
	}

	public String getPlanAction() {
		return this.plannerAction;
	}
	
	public void setDeclMonitoringStateCounts(Map<MonitoringState, Integer> declMonitoringStateCounts) {
		this.declMonitoringStateCounts = declMonitoringStateCounts;
	}
	public Map<MonitoringState, Integer> getDeclMonitoringStateCounts() {
		return declMonitoringStateCounts;
	}
	
	public boolean isStart() {
		return isStart;
	}
	public boolean isEnd() {
		return isEnd;
	}

	@Override
	public String toString() {
		return "EventData [eventNumber=" + eventNumber + ", activityName=" + activityName
				+ ", declMonitoringStateCounts=" + declMonitoringStateCounts + ", isStart=" + isStart + ", isEnd="
				+ isEnd + "]";
	}	
}
