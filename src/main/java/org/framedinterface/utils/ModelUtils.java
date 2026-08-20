package org.framedinterface.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.BidiMap;
import org.processmining.datapetrinets.DataPetriNetsWithMarkings;
import org.processmining.datapetrinets.io.DPNIOException;
import org.processmining.datapetrinets.io.DataPetriNetImporter;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import org.framedinterface.model.DeclareModel;
import org.framedinterface.model.PnModel;
import org.framedinterface.utils.enums.DeclareTemplate;
import org.framedinterface.model.DeclareConstraint;

public class ModelUtils {

	private ModelUtils() {
		//Private constructor to avoid unnecessary instantiation of the class
	}

	//Handles loading of a Declare model
	public static DeclareModel loadDeclareModel(Path modelPath, String modelId, String modelName) throws IOException {
		LinkedHashSet<String> activities = createActivityNamesSet(modelPath);
		BidiMap<String, String> activityToEncodingMap = createActivityToEncodingMap(activities);
		LinkedHashSet<DeclareConstraint> declareConstrains = readConstraints(modelPath);
		Map<String, List<DeclareConstraint>> activityToUnaryMap = createActivityToUnaryMap(activities, declareConstrains);
		Map<String, LinkedHashSet<String>> activityToAttributesMap = readActivityAttributeBindings(modelPath);
		addAttributesFromConditions(activityToAttributesMap, declareConstrains);

		DeclareModel declareModel = new DeclareModel(modelId, modelName, activities, activityToEncodingMap, declareConstrains, activityToUnaryMap, activityToAttributesMap);
		declareModel.setFilePath(modelPath.toAbsolutePath().toString());
		return declareModel;
	}

	//Creates activity names set based on Declare or LTL model
	private static LinkedHashSet<String> createActivityNamesSet(Path modelPath) throws IOException {
		LinkedHashSet<String> activityNames = new LinkedHashSet<String>();

		Scanner sc = new Scanner(modelPath);
		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			if(line.startsWith("activity ") && line.length() > 9) {
				//activityNames.add(line.substring(9));
				activityNames.add(line.substring(9).toLowerCase());
			}
		}
		sc.close();

		return activityNames;
	}
	
	
	private static BidiMap<String, String> createActivityToEncodingMap(LinkedHashSet<String> activities) {
		BidiMap<String, String> activityToEncodingMap =  new DualHashBidiMap<String, String>(); //I am not sure if this provides predictable iteration order; if yes, then activities set could be removed
		for (String activity : activities) {
			activityToEncodingMap.putIfAbsent(activity.toLowerCase(), "n"+activityToEncodingMap.size());
			//activityToEncodingMap.putIfAbsent(activity, "n"+activityToEncodingMap.size());
		}
		return activityToEncodingMap;
	}
	
	private static Map<String, List<DeclareConstraint>> createActivityToUnaryMap(LinkedHashSet<String> activities, LinkedHashSet<DeclareConstraint> declareConstrains) {
		Map<String, List<DeclareConstraint>> activityToUnaryMap = new HashMap<String, List<DeclareConstraint>>();
		for (DeclareConstraint declareConstraint : declareConstrains) {
			if (!declareConstraint.getTemplate().getIsBinary()) {
				activityToUnaryMap.putIfAbsent(declareConstraint.getActivationActivity(), new ArrayList<DeclareConstraint>());
				activityToUnaryMap.get(declareConstraint.getActivationActivity()).add(declareConstraint);
			}
		}
		return activityToUnaryMap;
	}
	
	//Finds "bind ActivityName: attr1, attr2" lines and creates a map of activity name to its bound attribute names
	private static Map<String, LinkedHashSet<String>> readActivityAttributeBindings(Path modelPath) throws IOException {
		Map<String, LinkedHashSet<String>> activityToAttributesMap = new HashMap<String, LinkedHashSet<String>>();

		Scanner sc = new Scanner(modelPath);
		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			if(line.startsWith("bind") && line.length() > 7 && line.substring(6).contains(":")) {
				String activity = line.substring(4, line.indexOf(':')).trim().toLowerCase();
				LinkedHashSet<String> attributes = activityToAttributesMap.computeIfAbsent(activity, a -> new LinkedHashSet<String>());
				for (String attributeName : line.substring(line.indexOf(':') + 1).split(",")) {
					if (!attributeName.trim().isEmpty()) {
						attributes.add(attributeName.trim().toLowerCase());
					}
				}
			}
		}
		sc.close();

		return activityToAttributesMap;
	}

	//Data conditions (e.g. "A.categorical is c2") also reveal attribute-activity associations, which "bind" lines don't always fully capture
	private static final Pattern CONDITION_ATTRIBUTE_PATTERN = Pattern.compile("\\b([AT])\\.(\\w+)");

	private static void addAttributesFromConditions(Map<String, LinkedHashSet<String>> activityToAttributesMap, LinkedHashSet<DeclareConstraint> declareConstraints) {
		for (DeclareConstraint declareConstraint : declareConstraints) {
			addAttributesFromCondition(activityToAttributesMap, declareConstraint, declareConstraint.getActivationCondition());
			addAttributesFromCondition(activityToAttributesMap, declareConstraint, declareConstraint.getTargetCondition());
		}
	}

	//"A." refers to the constraint's activation activity and "T." to its target activity, regardless of which condition string the token appears in
	private static void addAttributesFromCondition(Map<String, LinkedHashSet<String>> activityToAttributesMap, DeclareConstraint declareConstraint, String condition) {
		if (condition == null || condition.isEmpty()) {
			return;
		}
		Matcher matcher = CONDITION_ATTRIBUTE_PATTERN.matcher(condition);
		while (matcher.find()) {
			String activity = "A".equals(matcher.group(1)) ? declareConstraint.getActivationActivity() : declareConstraint.getTargetActivity();
			if (activity != null && !activity.isEmpty()) {
				activityToAttributesMap.computeIfAbsent(activity.toLowerCase(), a -> new LinkedHashSet<String>()).add(matcher.group(2).toLowerCase());
			}
		}
	}

	//Finds constraint strings in the Declare model and creates a list of Declare constraint objects
	private static LinkedHashSet<DeclareConstraint> readConstraints(Path declareModelPath) throws IOException {
		LinkedHashSet<DeclareConstraint> declareConstraints = new LinkedHashSet<DeclareConstraint>(); //To remove duplicates, while retaining predictable iteration order

		Scanner sc = new Scanner(declareModelPath);
		Pattern constraintPattern = Pattern.compile("\\w+\\[.*\\]"); //Trailing " |conditions" is optional

		while(sc.hasNextLine()) {
			String line = sc.nextLine();

			if(line.startsWith("activity") && line.length() > 9) {
				//Skipping activity definitions
			} else if(line.startsWith("bind") && line.length() > 7 && line.substring(6).contains(":")) {
				//Skipping activity-attribute bindings
			} else {
				Matcher constraintMatcher = constraintPattern.matcher(line);
				if (constraintMatcher.find()) { //Constraints
					DeclareConstraint declareConstraint = readConstraintString(line);
					declareConstraints.contains(declareConstraint);
					declareConstraints.add(declareConstraint);
				}
			}
		}
		sc.close();

		return declareConstraints;
	}

	//Creates a Declare constraint object from a single Declare constraint string
	private static DeclareConstraint readConstraintString(String constraintString) {
		DeclareTemplate template = null;
		String activationActivity = "";
		String targetActivity = "";
		String activationCondition = "";
		String targetCondition = "";
		String timeCondition = "";

		//The trailing " |activationCondition |targetCondition |timeCondition" section is optional -
		//some exports omit it entirely when a constraint has no data/time conditions
		Matcher mBinary = Pattern.compile("(.*)\\[(.*), (.*)\\](?: \\|(.*) \\|(.*) \\|(.*))?").matcher(constraintString);
		Matcher mUnary = Pattern.compile(".*\\[(.*)\\](?: \\|(.*) \\|(.*))?").matcher(constraintString);

		//Processing the constraint
		if(mBinary.find()) { //Binary constraints
			template = DeclareTemplate.getByTemplateName(mBinary.group(1));
			if(template.getReverseActivationTarget()) {
				targetActivity = mBinary.group(2);
				activationActivity = mBinary.group(3);
			}
			else {
				activationActivity = mBinary.group(2);
				targetActivity = mBinary.group(3);
			}
			activationCondition = trimOrEmpty(mBinary.group(4));
			targetCondition = trimOrEmpty(mBinary.group(5));
			timeCondition = trimOrEmpty(mBinary.group(6));
		} else if(mUnary.find()) { //Unary constraints
			template = DeclareTemplate.getByTemplateName(mUnary.group(0).substring(0, mUnary.group(0).indexOf("["))); //TODO: Should be done more intelligently
			activationActivity = mUnary.group(1);
			activationCondition = trimOrEmpty(mUnary.group(2));
			timeCondition = trimOrEmpty(mUnary.group(3));
		}

		return new DeclareConstraint(constraintString, template, activationActivity.toLowerCase(), targetActivity.toLowerCase(),
				activationCondition, targetCondition, timeCondition);
	}

	//Regex groups for the optional trailing condition section are null when that section is absent entirely
	private static String trimOrEmpty(String s) {
		return s == null ? "" : s.trim();
	}



	//Handles loading of a PN model
	public static PnModel loadDpnModel(Path modelPath, String modelId, String modelName) throws FileNotFoundException, DPNIOException {
		DataPetriNetImporter dataPetriNetImporter = new DataPetriNetImporter(); //There should be a version for regular Petri nets somewhere, but reusing the data Petri nets one will hopefully be fine
		InputStream inputStream = new BufferedInputStream(new FileInputStream(modelPath.toString()));
		DataPetriNetsWithMarkings dataPetriNet = dataPetriNetImporter.importFromStream(inputStream).getDPN();
		
		for (Transition transition : dataPetriNet.getTransitions()) {
			transition.getAttributeMap().put(AttributeMap.LABEL, transition.getLabel().toLowerCase());
		}
		
		LinkedHashSet<String> activities = createActivityNamesSet(dataPetriNet); //It is probably not necessary to encode the activity names of the Petri net
		BidiMap<String, String> activityToEncodingMap = createActivityToEncodingMap(activities);
		
		

		PnModel dpnModel = new PnModel(modelId, modelName, activities, activityToEncodingMap, dataPetriNet);
		dpnModel.setFilePath(modelPath.toAbsolutePath().toString());
		return dpnModel;
	}

	//Creates activity names set based on DPN model
	private static LinkedHashSet<String> createActivityNamesSet(DataPetriNetsWithMarkings dataPetriNet) {
		LinkedHashSet<String> activityNames = new LinkedHashSet<String>();
		for (Transition transition : dataPetriNet.getTransitions()) {
			if (!transition.isInvisible()) {
				activityNames.add(transition.getLabel().toLowerCase());
			}
		}
		return activityNames;
	}

}
