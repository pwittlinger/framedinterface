package org.framedinterface.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Evaluates a single DECLARE data condition (e.g. "A.integer < 20", "A.categorical is c2") against the attribute values
//attached to the trace event being checked. Used only for Data-Aware page replay - the Data-Agnostic page ignores data
//conditions entirely and keeps replaying purely on activity names.
public class DataConditionEvaluator {

	private DataConditionEvaluator() {
		//Private constructor to avoid unnecessary instantiation of the class
	}

	//"is not" must be tried before "is" in the alternation, otherwise "is" would match first and leave a dangling " not"
	private static final Pattern CLAUSE_PATTERN = Pattern.compile("\\b[AT]\\.(\\w+)\\s*(>=|<=|!=|=|>|<|is not|is)\\s*(\\S+)", Pattern.CASE_INSENSITIVE);

	//Some Declare exports emit a stray trailing delimiter (e.g. "|") for constraints with no conditions at all
	//(the "|"-separated activationCondition/targetCondition/timeCondition sections in the .decl source can end up
	//with one extra "|" that the field-splitting regex has nowhere else to put), so such punctuation/whitespace-only
	//content must not be mistaken for an actual (and therefore unsatisfiable) condition
	private static final Pattern MEANINGFUL_CONTENT_PATTERN = Pattern.compile("[A-Za-z0-9]");

	//A blank/absent condition is vacuously satisfied. Multiple clauses (as in "A.x > 1 and A.y is c2") are all required to hold.
	public static boolean evaluate(String condition, Map<String, String> attributeValues) {
		if (condition == null || condition.isBlank()) {
			return true;
		}

		Matcher matcher = CLAUSE_PATTERN.matcher(condition);
		boolean matchedAnyClause = false;
		while (matcher.find()) {
			matchedAnyClause = true;
			if (!evaluateClause(matcher.group(1).toLowerCase(), matcher.group(2).toLowerCase(), matcher.group(3), attributeValues)) {
				return false;
			}
		}
		if (matchedAnyClause) {
			return true;
		}
		//No clause was recognized - vacuously true if it's just stray delimiter/whitespace noise, otherwise treated as
		//an unsatisfied (unparseable) condition rather than silently ignored
		return !MEANINGFUL_CONTENT_PATTERN.matcher(condition).find();
	}

	private static boolean evaluateClause(String attributeName, String operator, String expectedValue, Map<String, String> attributeValues) {
		String actualValue = attributeValues == null ? null : attributeValues.get(attributeName);
		if (actualValue == null || actualValue.isBlank()) {
			return false; //No value recorded for this attribute on this event, so the condition cannot be confirmed
		}
		actualValue = actualValue.trim();

		if (operator.equals("is") || operator.equals("is not")) {
			boolean equal = actualValue.equalsIgnoreCase(expectedValue.trim());
			return operator.equals("is") ? equal : !equal;
		}

		try {
			double actual = Double.parseDouble(actualValue);
			double expected = Double.parseDouble(expectedValue.trim());
			switch (operator) {
				case ">": return actual > expected;
				case "<": return actual < expected;
				case ">=": return actual >= expected;
				case "<=": return actual <= expected;
				case "=": return actual == expected;
				case "!=": return actual != expected;
				default: return false;
			}
		} catch (NumberFormatException e) {
			return false; //Non-numeric attribute value compared with a numeric operator
		}
	}
}
