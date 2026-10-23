package org.framedinterface.model;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.BidiMap;

public abstract class AbstractModel {
	private String modelId;
	private String modelName;
	private ModelType modelType;
	private LinkedHashSet<String> activities; //For predictable iteration order
	private BidiMap<String, String> activityToEncodingMap; //To allow lookup by both keys and values
	private Map<String, String> activityNamesAsDefined = new HashMap<String, String>(); //Lowercase activity name (as used everywhere else in this class) -> the name as spelled in the model file
	public String fullFilePath;
	
	public AbstractModel(String modelId, String modelName, LinkedHashSet<String> activities, BidiMap<String, String> activityToEncodingMap, ModelType modelType) {
		this.modelId = modelId;
		this.modelName = modelName;
		this.activities = activities;
		this.activityToEncodingMap = activityToEncodingMap;
		this.modelType = modelType;
	}
	
	public String getModelId() {
		return modelId;
	}
	
	public String getModelName() {
		return modelName;
	}
	
	public ModelType getModelType() {
		return modelType;
	}
	
	public LinkedHashSet<String> getActivities() {
		return activities;
	}
	
	public String getActivityEncoding(String activity) {
		return activityToEncodingMap.get(activity);
	}
	
	public String getActivityByEncoding(String encoding) {
		return activityToEncodingMap.getKey(encoding);
	}
	
	public BidiMap<String, String> getActivityToEncodingMap() {
		return activityToEncodingMap;
	}

	//Activities are handled in lowercase internally, but external tools (e.g. the data-aware PDDL generator) match them case-sensitively against the model file
	//Returns the activity's name as spelled in the model file (case-insensitive lookup), or null if the model doesn't contain it
	public String getActivityNameAsDefined(String activity) {
		return activity == null ? null : activityNamesAsDefined.get(activity.toLowerCase());
	}

	public void setActivityNamesAsDefined(Map<String, String> activityNamesAsDefined) {
		this.activityNamesAsDefined = activityNamesAsDefined;
	}

	public void setFilePath(String filePath) {
		this.fullFilePath = filePath;
	}
	
	public String getFilePath(){
		return this.fullFilePath;
	}
	
	public abstract void updateMonitoringStates(List<String> activities, boolean displayViolations);
	
	public abstract String getVisualisationString(int activityIndex, boolean displayViolations);

	@Override
	public String toString() {
		return "AbstractModel [modelName=" + modelName + ", modelType=" + modelType + "]";
	}

	public abstract void resetModel();
	
}
