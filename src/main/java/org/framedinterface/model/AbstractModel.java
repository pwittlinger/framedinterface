package org.framedinterface.model;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.BidiMap;

/**
 * A process specification loaded into FrAIm: a Declare model ({@link DeclareModel}) or a Petri net ({@link PnModel}), with its id, name,
 * file and activities. Activities are lowercased. Each also has an encoding, its name in the model's visualization, through which a click
 * on the visualization is mapped back to the activity.
 * <p>
 * Subclasses replay traces on the model (see {@link #updateMonitoringStates(List, boolean)}) and visualize the model's state at each
 * position of the replayed trace (see {@link #getVisualisationString(int, boolean)}).
 */
public abstract class AbstractModel {
	private String modelId;
	private String modelName;
	private ModelType modelType;
	private LinkedHashSet<String> activities; //For predictable iteration order
	private BidiMap<String, String> activityToEncodingMap; //To allow lookup by both keys and values
	private Map<String, String> activityNamesAsDefined = new HashMap<String, String>(); //Lowercase activity name (as used everywhere else in this class) -> the name as spelled in the model file
	public String fullFilePath;
	
	/**
	 * Creates a model.
	 *
	 * @param modelId the model's id, unique among the loaded models (see {@link ModelRegistry#nextModelId()})
	 * @param modelName the model's name (its file name)
	 * @param activities the model's activities, lowercased, in a predictable order
	 * @param activityToEncodingMap each activity's encoding in the visualization, e.g. {@code n0}
	 * @param modelType whether it's a Declare model or a Petri net
	 */
	public AbstractModel(String modelId, String modelName, LinkedHashSet<String> activities, BidiMap<String, String> activityToEncodingMap, ModelType modelType) {
		this.modelId = modelId;
		this.modelName = modelName;
		this.activities = activities;
		this.activityToEncodingMap = activityToEncodingMap;
		this.modelType = modelType;
	}
	
	/**
	 * Returns the model's id, which also identifies its graph in the visualization.
	 *
	 * @return the id, unique among the loaded models
	 */
	public String getModelId() {
		return modelId;
	}
	
	/**
	 * Returns the model's name, as shown in the tables and choice boxes.
	 *
	 * @return the name, i.e. the model's file name
	 */
	public String getModelName() {
		return modelName;
	}
	
	/**
	 * Returns whether this is a Declare model or a Petri net.
	 *
	 * @return the model's type
	 */
	public ModelType getModelType() {
		return modelType;
	}
	
	/**
	 * Returns the model's activities.
	 *
	 * @return the activities, lowercased
	 */
	public LinkedHashSet<String> getActivities() {
		return activities;
	}
	
	/**
	 * Returns an activity's encoding, its name in the model's visualization.
	 *
	 * @param activity the activity, lowercased
	 * @return the encoding, e.g. {@code n0}, or {@code null} if the model has no such activity
	 */
	public String getActivityEncoding(String activity) {
		return activityToEncodingMap.get(activity);
	}
	
	/**
	 * Returns the activity with the given encoding, e.g. the one clicked on in the visualization.
	 *
	 * @param encoding the encoding, e.g. {@code n0}
	 * @return the activity, lowercased, or {@code null} if no activity has that encoding
	 */
	public String getActivityByEncoding(String encoding) {
		return activityToEncodingMap.getKey(encoding);
	}
	
	/**
	 * Returns the encodings of all activities.
	 *
	 * @return activity (lowercased) to encoding; also looks up activities by encoding
	 */
	public BidiMap<String, String> getActivityToEncodingMap() {
		return activityToEncodingMap;
	}

	/**
	 * Returns an activity's name as spelled in the model file. Activities are handled in lowercase internally, but external tools (e.g. the
	 * data-aware PDDL generator) match them case-sensitively against the model file.
	 *
	 * @param activity the activity, in any casing (looked up case-insensitively); may be {@code null}
	 * @return the activity's name as spelled in the model file, or {@code null} if the model doesn't contain it
	 */
	public String getActivityNameAsDefined(String activity) {
		return activity == null ? null : activityNamesAsDefined.get(activity.toLowerCase());
	}

	/**
	 * Sets how the model file spells each activity (see {@link #getActivityNameAsDefined(String)}).
	 *
	 * @param activityNamesAsDefined lowercased activity name to its spelling in the model file
	 */
	public void setActivityNamesAsDefined(Map<String, String> activityNamesAsDefined) {
		this.activityNamesAsDefined = activityNamesAsDefined;
	}

	/**
	 * Sets the model's file.
	 *
	 * @param filePath the model file's absolute path
	 */
	public void setFilePath(String filePath) {
		this.fullFilePath = filePath;
	}
	
	/**
	 * Returns the model's file, e.g. to hand it to the PDDL generator.
	 *
	 * @return the model file's absolute path
	 */
	public String getFilePath(){
		return this.fullFilePath;
	}
	
	/**
	 * Replays the given trace on the model, and records the model's state at each position of the trace: before the first entry, after
	 * each entry, and at the trace's end.
	 *
	 * @param activities the trace: activities, or plan steps {@code action;activity}
	 * @param displayViolations whether past violations are to be shown; not used by the current model types (the choice is made by
	 *        {@link #getVisualisationString(int, boolean)})
	 */
	public abstract void updateMonitoringStates(List<String> activities, boolean displayViolations);
	
	/**
	 * Returns the model's visualization, in Graphviz DOT, at the given position of the last replayed trace.
	 *
	 * @param activityIndex the position: 0 before the first entry, then one per entry, and the last one at the trace's end
	 * @param displayViolations whether to also show the violations that occurred earlier in the trace
	 * @return the visualization, ready to be passed to the visualization page's JavaScript
	 */
	public abstract String getVisualisationString(int activityIndex, boolean displayViolations);

	/**
	 * Returns a description of the model for debugging: its name and type.
	 *
	 * @return the description
	 */
	@Override
	public String toString() {
		return "AbstractModel [modelName=" + modelName + ", modelType=" + modelType + "]";
	}

	/**
	 * Returns the model to its initial state, before any event of a trace.
	 */
	public abstract void resetModel();
	
}
