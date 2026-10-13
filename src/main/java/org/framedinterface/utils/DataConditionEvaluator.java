package org.framedinterface.utils;

import java.util.Map;

import formula.Formula;
import formula.Formula.Comparison;
import formula.FormulaParseException;
import formula.Formulas;
import model.Guard;

//Evaluates a DECLARE data condition (e.g. "A.integer < 20", "A.categorical is c2 or not A.integer > 5") against the
//attribute values of the trace event we're checking. Used only for the replay on the Data-Aware page, the Data-Agnostic
//page ignores data conditions and replays only on activity names.
//The parsing comes from the generator (pddl_gen), so here we read the conditions exactly like the planner does.
public class DataConditionEvaluator {

	private DataConditionEvaluator() {
		//Private constructor to avoid unnecessary instantiation of the class
	}

	//No condition means it always holds. Conditions can be any boolean formula (and, or, not, ->, in, ...).
	public static boolean evaluate(String condition, Map<String, String> attributeValues) {
		Formula guard;
		try {
			//Guard.parse also gives back null for leftover delimiters like a stray "|"
			guard = Guard.parse(condition);
		} catch (FormulaParseException e) {
			System.err.println("Could not read the condition \"" + condition + "\": " + e.getMessage());
			return false; //We'd rather show it as not satisfied than silently ignore it
		}
		if (guard == null) {
			return true;
		}

		//Like in the planner: if the event doesn't have a value the condition talks about, the condition doesn't hold
		for (Comparison comparison : Formulas.comparisons(guard)) {
			if (valueOf(comparison, attributeValues) == null) {
				return false;
			}
		}
		return Formulas.evaluate(guard, literal -> holds((Comparison) literal, attributeValues));
	}

	private static String valueOf(Comparison comparison, Map<String, String> attributeValues) {
		String value = attributeValues == null ? null : attributeValues.get(comparison.attribute().toLowerCase());
		return value == null || value.isBlank() ? null : value.trim();
	}

	private static boolean holds(Comparison comparison, Map<String, String> attributeValues) {
		String actualValue = valueOf(comparison, attributeValues);

		//Categorical values (after Guard.parse "is" and "in" are just = and !=)
		if (!(comparison.value() instanceof Number expected)) {
			boolean equal = actualValue.equalsIgnoreCase(String.valueOf(comparison.value()));
			return comparison.operator().equals("=") ? equal : comparison.operator().equals("!=") && !equal;
		}

		double actual;
		try {
			actual = Double.parseDouble(actualValue);
		} catch (NumberFormatException e) {
			return false; //Non-numeric attribute value compared with a number
		}
		double value = expected.doubleValue();
		switch (comparison.operator()) {
			case ">": return actual > value;
			case "<": return actual < value;
			case ">=": return actual >= value;
			case "<=": return actual <= value;
			case "=": return actual == value;
			case "!=": return actual != value;
			default: return false;
		}
	}
}
