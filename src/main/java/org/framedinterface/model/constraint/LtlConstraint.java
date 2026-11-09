package org.framedinterface.model.constraint;

import java.util.List;

/**
 * An LTL constraint: an LTL formula with its data conditions.
 * <p>
 * This class isn't used anywhere in FrAIm, nor is the rest of its package (see {@link DeclareConstraint}). It has been unchanged since
 * the project's first commit.
 */
public class LtlConstraint {
	String formulaString;
	List<String> dataConditions;
	
	/**
	 * Creates a constraint.
	 *
	 * @param formulaString the LTL formula
	 * @param dataConditions the formula's data conditions
	 */
	public LtlConstraint(String formulaString, List<String> dataConditions) {
		this.formulaString = formulaString;
		this.dataConditions = dataConditions;
	}
	
	/**
	 * Returns the LTL formula.
	 *
	 * @return the formula
	 */
	public String getFormulaString() {
		return formulaString;
	}
	
	/**
	 * Returns the formula's data conditions.
	 *
	 * @return the data conditions
	 */
	public List<String> getDataConditions() {
		return dataConditions;
	}

	/**
	 * Returns a description of the constraint for debugging: its formula and data conditions.
	 *
	 * @return the description
	 */
	@Override
	public String toString() {
		return "LtlConstraint [formulaString=" + formulaString + ", dataConditions=" + dataConditions + "]";
	}
	
}
