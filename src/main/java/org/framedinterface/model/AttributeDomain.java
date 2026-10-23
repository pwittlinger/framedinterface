package org.framedinterface.model;

import java.util.Set;
import java.util.TreeSet;

//The possible values an attribute can take, as declared in a Declare model ("<attr>: integer between <lo> and <hi>" or "<attr>: v1, v2, ...")
public class AttributeDomain {

	public enum Type {
		INTEGER, FLOAT, CATEGORICAL
	}

	private Type type;
	private double lowerBound;
	private double upperBound;
	private Set<String> values;

	private AttributeDomain() {
	}

	public static AttributeDomain numeric(Type type, double lowerBound, double upperBound) {
		AttributeDomain domain = new AttributeDomain();
		domain.type = type;
		domain.lowerBound = lowerBound;
		domain.upperBound = upperBound;
		return domain;
	}

	public static AttributeDomain categorical(Set<String> values) {
		AttributeDomain domain = new AttributeDomain();
		domain.type = Type.CATEGORICAL;
		domain.values = values;
		return domain;
	}

	public Type getType() {
		return type;
	}

	public double getLowerBound() {
		return lowerBound;
	}

	public double getUpperBound() {
		return upperBound;
	}

	public Set<String> getValues() {
		return values;
	}

	//Widest domain covering both inputs - used when the same attribute is declared by multiple selected models
	public static AttributeDomain merge(AttributeDomain a, AttributeDomain b) {
		if (a.type == Type.CATEGORICAL) {
			Set<String> merged = new TreeSet<String>(a.values);
			merged.addAll(b.values);
			return categorical(merged);
		}
		return numeric(a.type, Math.min(a.lowerBound, b.lowerBound), Math.max(a.upperBound, b.upperBound));
	}

	//Text shown under the attribute's name in the Attributes tree
	public String describe() {
		if (type == Type.CATEGORICAL) {
			return String.join(", ", values);
		}
		return "between " + formatNumber(lowerBound) + " and " + formatNumber(upperBound);
	}

	private static String formatNumber(double value) {
		return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
	}

}
