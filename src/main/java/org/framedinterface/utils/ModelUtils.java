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
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.BidiMap;
import org.processmining.datapetrinets.DataPetriNetsWithMarkings;
import org.processmining.datapetrinets.io.DPNIOException;
import org.processmining.datapetrinets.io.DataPetriNetImporter;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import org.framedinterface.model.AttributeDomain;
import org.framedinterface.model.DeclareModel;
import org.framedinterface.model.PnModel;
import org.framedinterface.utils.enums.DeclareTemplate;
import org.framedinterface.model.DeclareConstraint;

/**
 * Loads process specifications from their files: Declare models ({@code .decl}) and Petri nets ({@code .pnml}).
 * <p>
 * A Declare model file has one statement per line, e.g.:
 * <pre>{@code
 * activity ActivityA
 * bind ActivityA: integer, categorical
 * integer: integer between 0 and 100
 * categorical: c1, c2, c3
 * Response[ActivityA, ActivityB] |A.integer > 10 |T.integer < 10 | ActivityA,0,100,h/ActivityB,2,5,h
 * }</pre>
 * A constraint's trailing section ({@code |activation condition |target condition |time condition}) is optional. Activity and attribute
 * names are lowercased; the spelling of the activities in the file is kept separately (see
 * {@link org.framedinterface.model.AbstractModel#getActivityNameAsDefined(String)}). The parsing is strict about whitespace: activity
 * names aren't trimmed, and a binary constraint's activities must be separated by a comma and a space.
 */
public class ModelUtils {

	/**
	 * Not meant to be instantiated: all methods are static.
	 */
	private ModelUtils() {
		//Private constructor to avoid unnecessary instantiation of the class
	}

	/**
	 * Loads a Declare model from its file: its activities, constraints, attribute bindings and attribute domains. Duplicate constraints
	 * are dropped (see {@link DeclareConstraint#equals(Object)}).
	 *
	 * @param modelPath the model file
	 * @param modelId the model's id, unique among the loaded models
	 * @param modelName the model's name, e.g. its file name
	 * @return the model
	 * @throws IOException if the file can't be read
	 * @throws NullPointerException if a constraint's template isn't known, e.g. a misspelled template name
	 * @throws NumberFormatException if a bound of a numeric attribute domain isn't a number
	 */
	public static DeclareModel loadDeclareModel(Path modelPath, String modelId, String modelName) throws IOException {
		LinkedHashSet<String> activities = createActivityNamesSet(modelPath);
		BidiMap<String, String> activityToEncodingMap = createActivityToEncodingMap(activities);
		LinkedHashSet<DeclareConstraint> declareConstrains = readConstraints(modelPath);
		Map<String, List<DeclareConstraint>> activityToUnaryMap = createActivityToUnaryMap(activities, declareConstrains);
		Map<String, LinkedHashSet<String>> activityToAttributesMap = readActivityAttributeBindings(modelPath);
		addAttributesFromConditions(activityToAttributesMap, declareConstrains);
		Map<String, AttributeDomain> attributeDomains = readAttributeDomains(modelPath);

		DeclareModel declareModel = new DeclareModel(modelId, modelName, activities, activityToEncodingMap, declareConstrains, activityToUnaryMap, activityToAttributesMap, attributeDomains);
		declareModel.setFilePath(modelPath.toAbsolutePath().toString());
		declareModel.setActivityNamesAsDefined(readActivityNamesAsDefined(modelPath));
		return declareModel;
	}

	/**
	 * Reads the activities declared by the {@code activity <name>} lines, lowercased. The names aren't trimmed, so trailing whitespace
	 * stays part of the name.
	 *
	 * @param modelPath the model file
	 * @return the activities, in file order
	 * @throws IOException if the file can't be read
	 */
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

	/**
	 * Maps each activity name of a Declare model, lowercased as in {@link #createActivityNamesSet(Path)}, to its original spelling in the
	 * model file (trimmed).
	 *
	 * @param modelPath the model file
	 * @return lowercased activity name to its spelling in the file
	 * @throws IOException if the file can't be read
	 */
	private static Map<String, String> readActivityNamesAsDefined(Path modelPath) throws IOException {
		Map<String, String> activityNamesAsDefined = new HashMap<String, String>();

		Scanner sc = new Scanner(modelPath);
		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			if(line.startsWith("activity ") && line.length() > 9) {
				activityNamesAsDefined.putIfAbsent(line.substring(9).toLowerCase(), line.substring(9).trim());
			}
		}
		sc.close();

		return activityNamesAsDefined;
	}
	
	
	/**
	 * Gives each activity an encoding, its node's id in the visualization: {@code n0}, {@code n1}, ... in the given order.
	 *
	 * @param activities the activities, lowercased
	 * @return activity to encoding; also looks up activities by encoding
	 */
	private static BidiMap<String, String> createActivityToEncodingMap(LinkedHashSet<String> activities) {
		BidiMap<String, String> activityToEncodingMap =  new DualHashBidiMap<String, String>(); //I am not sure if this provides predictable iteration order; if yes, then activities set could be removed
		for (String activity : activities) {
			activityToEncodingMap.putIfAbsent(activity.toLowerCase(), "n"+activityToEncodingMap.size());
			//activityToEncodingMap.putIfAbsent(activity, "n"+activityToEncodingMap.size());
		}
		return activityToEncodingMap;
	}
	
	/**
	 * Groups the unary constraints (e.g. Existence or Init) by their activity, so they can be shown on the activity's node.
	 *
	 * @param activities not used
	 * @param declareConstrains the model's constraints
	 * @return activity (lowercased) to its unary constraints, in file order; activities without any aren't in the map
	 */
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
	
	/**
	 * Reads the {@code bind ActivityName: attr1, attr2} lines, which bind attributes to an activity.
	 *
	 * @param modelPath the model file
	 * @return activity (lowercased) to its bound attributes (lowercased), in file order
	 * @throws IOException if the file can't be read
	 */
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

	/**
	 * Adds the attributes the constraints' data conditions refer to (e.g. {@code A.categorical is c2}) to their activities, as the "bind"
	 * lines don't always fully capture them.
	 *
	 * @param activityToAttributesMap activity (lowercased) to its attributes, which is added to
	 * @param declareConstraints the model's constraints
	 */
	private static void addAttributesFromConditions(Map<String, LinkedHashSet<String>> activityToAttributesMap, LinkedHashSet<DeclareConstraint> declareConstraints) {
		for (DeclareConstraint declareConstraint : declareConstraints) {
			addAttributesFromCondition(activityToAttributesMap, declareConstraint, declareConstraint.getActivationCondition());
			addAttributesFromCondition(activityToAttributesMap, declareConstraint, declareConstraint.getTargetCondition());
		}
	}

	/**
	 * Adds the attributes one data condition refers to. "A." refers to the constraint's activation activity and "T." to its target
	 * activity, regardless of which condition string the token appears in.
	 *
	 * @param activityToAttributesMap activity (lowercased) to its attributes, which is added to
	 * @param declareConstraint the constraint the condition belongs to
	 * @param condition the condition; may be {@code null} or empty
	 */
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

	//Matches attribute domain declaration lines, e.g. "integer: integer between 0 and 100" or "categorical: c1, c2, c3"
	//(these are the only remaining colon-lines once "activity X" and "bind X: ..." lines are excluded, since neither has a bare "word:" prefix)
	private static final Pattern DOMAIN_LINE_PATTERN = Pattern.compile("^(\\w+):\\s*(.*)$");
	private static final Pattern NUMERIC_DOMAIN_PATTERN = Pattern.compile("(?i)(integer|float)\\s+between\\s+(\\S+)\\s+and\\s+(\\S+)");

	/**
	 * Reads the attribute domain declarations: the {@code <word>: ...} lines, e.g. {@code integer: integer between 0 and 100} or
	 * {@code categorical: c1, c2, c3}. A description containing {@code integer} or {@code float between <lo> and <hi>} declares a numeric
	 * range; any other non-empty one, a set of comma-separated categorical values.
	 *
	 * @param modelPath the model file
	 * @return attribute name (lowercased) to its declared value range or set
	 * @throws IOException if the file can't be read
	 * @throws NumberFormatException if a range's bound isn't a number
	 */
	private static Map<String, AttributeDomain> readAttributeDomains(Path modelPath) throws IOException {
		Map<String, AttributeDomain> attributeDomains = new HashMap<String, AttributeDomain>();

		Scanner sc = new Scanner(modelPath);
		while (sc.hasNextLine()) {
			Matcher domainMatcher = DOMAIN_LINE_PATTERN.matcher(sc.nextLine());
			if (domainMatcher.matches()) {
				String attributeName = domainMatcher.group(1).toLowerCase();
				String description = domainMatcher.group(2).trim();
				Matcher numericMatcher = NUMERIC_DOMAIN_PATTERN.matcher(description);
				if (numericMatcher.find()) {
					AttributeDomain.Type type = "float".equalsIgnoreCase(numericMatcher.group(1)) ? AttributeDomain.Type.FLOAT : AttributeDomain.Type.INTEGER;
					attributeDomains.put(attributeName, AttributeDomain.numeric(type, Double.parseDouble(numericMatcher.group(2)), Double.parseDouble(numericMatcher.group(3))));
				} else if (!description.isEmpty()) {
					Set<String> values = new TreeSet<String>();
					for (String value : description.split(",")) {
						if (!value.trim().isEmpty()) {
							values.add(value.trim());
						}
					}
					if (!values.isEmpty()) {
						attributeDomains.put(attributeName, AttributeDomain.categorical(values));
					}
				}
			}
		}
		sc.close();

		return attributeDomains;
	}

	/**
	 * Reads the constraints: every line, other than the {@code activity} and {@code bind} lines, that contains something like
	 * {@code Template[...]}. Duplicate constraints are dropped, keeping the first (see {@link DeclareConstraint#equals(Object)}).
	 *
	 * @param declareModelPath the model file
	 * @return the constraints, in file order
	 * @throws IOException if the file can't be read
	 * @throws NullPointerException if a constraint's template isn't known
	 */
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

	/**
	 * Creates a Declare constraint from its line, e.g.
	 * {@code Response[ActivityG, ActivityP] |A.integer > 10 |T.integer < 10 | ActivityP,0,100,h/ActivityG,2,5,h}. A binary constraint's
	 * activities must be separated by a comma and a space; otherwise the line is read as a unary constraint whose activity is both of
	 * them. For the templates that reverse activation and target, the activation is the second activity.
	 *
	 * @param constraintString the constraint's line
	 * @return the constraint, with lowercased activities and trimmed conditions
	 * @throws NullPointerException if the template isn't known
	 */
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

	/**
	 * Trims a regex group of the optional trailing condition section; the groups are {@code null} when that section is absent entirely.
	 *
	 * @param s the group, may be {@code null}
	 * @return the trimmed group, or an empty text for {@code null}
	 */
	private static String trimOrEmpty(String s) {
		return s == null ? "" : s.trim();
	}



	/**
	 * Loads a Petri net from its PNML file, as a data Petri net. All transition labels are lowercased in the net itself; the original
	 * spelling of the visible transitions' labels is kept separately (see
	 * {@link org.framedinterface.model.AbstractModel#getActivityNameAsDefined(String)}). The file's input stream isn't closed, so the
	 * file stays open (and on Windows can't be deleted) until the stream is garbage-collected.
	 *
	 * @param modelPath the model file
	 * @param modelId the model's id, unique among the loaded models
	 * @param modelName the model's name, e.g. its file name
	 * @return the model
	 * @throws FileNotFoundException if the file doesn't exist
	 * @throws DPNIOException if the file can't be read as a data Petri net
	 */
	public static PnModel loadDpnModel(Path modelPath, String modelId, String modelName) throws FileNotFoundException, DPNIOException {
		DataPetriNetImporter dataPetriNetImporter = new DataPetriNetImporter(); //There should be a version for regular Petri nets somewhere, but reusing the data Petri nets one will hopefully be fine
		InputStream inputStream = new BufferedInputStream(new FileInputStream(modelPath.toString()));
		DataPetriNetsWithMarkings dataPetriNet = dataPetriNetImporter.importFromStream(inputStream).getDPN();

		Map<String, String> activityNamesAsDefined = new HashMap<String, String>(); //Must be captured before the labels are lowercased below
		for (Transition transition : dataPetriNet.getTransitions()) {
			if (!transition.isInvisible()) {
				activityNamesAsDefined.putIfAbsent(transition.getLabel().toLowerCase(), transition.getLabel());
			}
			transition.getAttributeMap().put(AttributeMap.LABEL, transition.getLabel().toLowerCase());
		}
		
		LinkedHashSet<String> activities = createActivityNamesSet(dataPetriNet); //It is probably not necessary to encode the activity names of the Petri net
		BidiMap<String, String> activityToEncodingMap = createActivityToEncodingMap(activities);
		
		

		PnModel dpnModel = new PnModel(modelId, modelName, activities, activityToEncodingMap, dataPetriNet);
		dpnModel.setFilePath(modelPath.toAbsolutePath().toString());
		dpnModel.setActivityNamesAsDefined(activityNamesAsDefined);
		return dpnModel;
	}

	/**
	 * Collects the labels of the net's visible transitions, lowercased.
	 *
	 * @param dataPetriNet the net
	 * @return the activities, in the net's transition order
	 */
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
