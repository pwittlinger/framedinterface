package org.framedinterface.utils;

import java.util.Map;

import formula.Formula;
import formula.Formula.Comparison;
import formula.FormulaParseException;
import formula.Formulas;
import model.Guard;

/**
 * Evaluates a Declare data condition, e.g. {@code A.integer < 20} or {@code A.categorical is c2 or not A.integer > 5}, against the
 * attribute values of the trace event being checked. Used only for the replay on the Data-Aware page; the Data-Agnostic page ignores data
 * conditions and replays only on activity names.
 * <p>
 * The parsing comes from the PDDL generator (pddl_gen's {@code model.Guard}), so the conditions are read exactly like the planner reads
 * them. A condition compares attributes with values; comparing two attributes with each other (e.g. {@code T.integer = A.integer})
 * isn't supported by that parser.
 */
public class DataConditionEvaluator {

	/**
	 * Not meant to be instantiated: all methods are static.
	 */
	private DataConditionEvaluator() {
		//Private constructor to avoid unnecessary instantiation of the class
	}

	/**
	 * Tells whether the given condition holds for an event with the given attribute values. No condition means it always holds.
	 * Conditions can be any boolean formula ({@code and}, {@code or}, {@code not}, {@code ->}, {@code in}, ...) over comparisons such as
	 * {@code A.integer < 20}, {@code A.categorical is c2} or {@code A.categorical in (c1, c2)}.
	 * <p>
	 * Like in the planner, if the event has no value for an attribute the condition talks about, the whole condition doesn't hold, even
	 * where the rest of it would: e.g. {@code A.categorical is c2 or A.integer > 5} doesn't hold for an event with only
	 * {@code categorical = c2}. A condition that can't be parsed doesn't hold either; the parse error is printed.
	 *
	 * @param condition the condition, with its {@code A.} or {@code T.} prefixes; empty for none
	 * @param attributeValues the event's attribute values, keyed by lowercased attribute name; may be {@code null}
	 * @return {@code true} if the condition holds
	 */
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

	/**
	 * Returns the event's value for the attribute a comparison talks about, looked up by its lowercased name.
	 *
	 * @param comparison the comparison
	 * @param attributeValues the event's attribute values; may be {@code null}
	 * @return the value without surrounding whitespace, or {@code null} if the event has none (a blank value counts as none)
	 */
	private static String valueOf(Comparison comparison, Map<String, String> attributeValues) {
		String value = attributeValues == null ? null : attributeValues.get(comparison.attribute().toLowerCase());
		return value == null || value.isBlank() ? null : value.trim();
	}

	/**
	 * Tells whether a single comparison holds for the event. Categorical values are compared case-insensitively, and only with {@code =}
	 * and {@code !=} (after parsing, {@code is}, {@code is not} and {@code in} are just those). Numeric values are compared as numbers; a
	 * non-numeric attribute value never satisfies a numeric comparison.
	 *
	 * @param comparison the comparison
	 * @param attributeValues the event's attribute values, which must contain the comparison's attribute (see
	 *        {@link #evaluate(String, Map)})
	 * @return {@code true} if the comparison holds
	 */
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
