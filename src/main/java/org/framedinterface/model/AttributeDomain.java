package org.framedinterface.model;

import java.util.Set;
import java.util.TreeSet;

/**
 * The possible values an attribute can take, as declared in a Declare model: a numeric range ({@code <attr>: integer between <lo> and <hi>},
 * or {@code float} instead of {@code integer}), or a set of categorical values ({@code <attr>: v1, v2, ...}). Read by
 * {@link org.framedinterface.utils.ModelUtils} when a model is loaded, and shown in the Data-Aware page's Attributes pane.
 */
public class AttributeDomain {

	/**
	 * The kind of an attribute's values: {@link #INTEGER} or {@link #FLOAT} for a numeric range, {@link #CATEGORICAL} for a set of values.
	 */
	public enum Type {
		INTEGER, FLOAT, CATEGORICAL
	}

	private Type type;
	private double lowerBound;
	private double upperBound;
	private Set<String> values;

	/**
	 * Only used by {@link #numeric(Type, double, double)} and {@link #categorical(Set)}.
	 */
	private AttributeDomain() {
	}

	/**
	 * Creates a numeric domain, e.g. for {@code integer: integer between 0 and 100}.
	 *
	 * @param type {@link Type#INTEGER} or {@link Type#FLOAT}
	 * @param lowerBound the range's lower bound
	 * @param upperBound the range's upper bound
	 * @return the domain
	 */
	public static AttributeDomain numeric(Type type, double lowerBound, double upperBound) {
		AttributeDomain domain = new AttributeDomain();
		domain.type = type;
		domain.lowerBound = lowerBound;
		domain.upperBound = upperBound;
		return domain;
	}

	/**
	 * Creates a categorical domain, e.g. for {@code categorical: c1, c2, c3}.
	 *
	 * @param values the values the attribute can take; the set is used as is, not copied
	 * @return the domain
	 */
	public static AttributeDomain categorical(Set<String> values) {
		AttributeDomain domain = new AttributeDomain();
		domain.type = Type.CATEGORICAL;
		domain.values = values;
		return domain;
	}

	/**
	 * Returns the kind of the attribute's values.
	 *
	 * @return the type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Returns the lower bound of a numeric domain's range.
	 *
	 * @return the lower bound; 0 for a categorical domain
	 */
	public double getLowerBound() {
		return lowerBound;
	}

	/**
	 * Returns the upper bound of a numeric domain's range.
	 *
	 * @return the upper bound; 0 for a categorical domain
	 */
	public double getUpperBound() {
		return upperBound;
	}

	/**
	 * Returns the values of a categorical domain.
	 *
	 * @return the values, or {@code null} for a numeric domain
	 */
	public Set<String> getValues() {
		return values;
	}

	/**
	 * Merges two domains of the same attribute into the widest domain covering both, for when the attribute is declared by several
	 * selected models: the union of their values, or the range from the smaller lower bound to the larger upper bound. The result has the
	 * type of {@code a}. Domains of different kinds aren't handled: if {@code a} is categorical and {@code b} numeric, this throws a
	 * {@link NullPointerException}; the other way round, {@code b}'s values are ignored and the range is widened to include 0.
	 *
	 * @param a the domain declared by one model
	 * @param b the domain declared by another model
	 * @return the merged domain; neither input is changed
	 */
	public static AttributeDomain merge(AttributeDomain a, AttributeDomain b) {
		if (a.type == Type.CATEGORICAL) {
			Set<String> merged = new TreeSet<String>(a.values);
			merged.addAll(b.values);
			return categorical(merged);
		}
		return numeric(a.type, Math.min(a.lowerBound, b.lowerBound), Math.max(a.upperBound, b.upperBound));
	}

	/**
	 * Describes the domain, as shown under the attribute's name in the Data-Aware page's Attributes pane.
	 *
	 * @return the values separated by commas, e.g. {@code c1, c2, c3}, or the range, e.g. {@code between 0 and 100}
	 */
	public String describe() {
		if (type == Type.CATEGORICAL) {
			return String.join(", ", values);
		}
		return "between " + formatNumber(lowerBound) + " and " + formatNumber(upperBound);
	}

	/**
	 * Formats a bound without a fraction if it's a whole number, e.g. "100" rather than "100.0".
	 *
	 * @param value the bound
	 * @return the formatted bound
	 */
	private static String formatNumber(double value) {
		return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
	}

}
