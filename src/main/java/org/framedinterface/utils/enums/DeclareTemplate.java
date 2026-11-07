package org.framedinterface.utils.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * The Declare templates FrAIm supports, each with the name it's written with in model files (e.g. {@code Not Chain Response}). In the
 * descriptions of the constants, A and B are the constraint's first and second activity as written in the model file.
 * <p>
 * Each template's LTL formula is in {@link org.framedinterface.utils.AutomatonUtils#getGenericLtlFormula(DeclareTemplate)}. For the
 * templates that reverse activation and target (the precedence templates, see {@link #getReverseActivationTarget()}), the activation is
 * the second activity, e.g. B in {@code Precedence[A, B]}.
 */
public enum DeclareTemplate {
	/** {@code Absence[A]}: A never occurs. */
	Absence("Absence", false, false, false),
	/** {@code Absence2[A]}: A occurs at most once. */
	Absence2("Absence2", false, false, false),
	/** {@code Absence3[A]}: A occurs at most twice. */
	Absence3("Absence3", false, false, false),
	/** {@code End[A]}: the trace ends with A. */
	End("End", false, false, false),
	/** {@code Exactly1[A]}: A occurs exactly once. */
	Exactly1("Exactly1", false, false, false),
	/** {@code Exactly2[A]}: A occurs exactly twice. */
	Exactly2("Exactly2", false, false, false),
	/** {@code Existence[A]}: A occurs at least once. */
	Existence("Existence", false, false, false),
	/** {@code Existence2[A]}: A occurs at least twice. */
	Existence2("Existence2", false, false, false),
	/** {@code Existence3[A]}: A occurs at least three times. */
	Existence3("Existence3", false, false, false), 
	/** {@code Init[A]}: the trace starts with A. */
	Init("Init", false, false, false),
	/** {@code Alternate Precedence[A, B]}: every B is preceded by an A, with no other B in between. */
	Alternate_Precedence("Alternate Precedence", true, false, true),
	/** {@code Alternate Response[A, B]}: every A is followed by a B, with no other A in between. */
	Alternate_Response("Alternate Response", true, false, false),
	/** {@code Alternate Succession[A, B]}: both Alternate Response and Alternate Precedence. */
	Alternate_Succession("Alternate Succession", true, false, false),
	/** {@code Chain Precedence[A, B]}: every B is directly preceded by an A. */
	Chain_Precedence("Chain Precedence", true, false, true),
	/** {@code Chain Response[A, B]}: every A is directly followed by a B. */
	Chain_Response("Chain Response", true, false, false),
	/** {@code Chain Succession[A, B]}: both Chain Response and Chain Precedence. */
	Chain_Succession("Chain Succession", true, false, false),
	/** {@code Co-Existence[A, B]}: if A occurs, B occurs too, and vice versa. */
	CoExistence("Co-Existence", true, false, false),
	/** {@code Precedence[A, B]}: B only occurs after an A has occurred. */
	Precedence("Precedence", true, false, true),
	/** {@code Responded Existence[A, B]}: if A occurs, B occurs too, before or after it. */
	Responded_Existence("Responded Existence", true, false, false),
	/** {@code Response[A, B]}: every A is eventually followed by a B. */
	Response("Response", true, false, false),
	/** {@code Succession[A, B]}: both Response and Precedence. */
	Succession("Succession", true, false, false),
	/** {@code Choice[A, B]}: A or B occurs, or both. */
	Choice("Choice", true, false, false),
	/** {@code Exclusive Choice[A, B]}: A or B occurs, but not both. */
	Exclusive_Choice("Exclusive Choice", true, false, false),
	/** {@code Not Chain Precedence[A, B]}: no B is directly preceded by an A. */
	Not_Chain_Precedence("Not Chain Precedence", true, true, true),
	/** {@code Not Chain Response[A, B]}: no A is directly followed by a B. */
	Not_Chain_Response("Not Chain Response", true, true, false),
	/** {@code Not Chain Succession[A, B]}: no A is directly followed by a B (in FrAIm, the same as Not Chain Response). */
	Not_Chain_Succession("Not Chain Succession", true, true, false),
	/** {@code Not Co-Existence[A, B]}: A and B don't both occur. */
	Not_CoExistence("Not Co-Existence", true, true, false),
	/** {@code Not Precedence[A, B]}: no B occurs after an A. */
	Not_Precedence("Not Precedence", true, true, true),
	/** {@code Not Responded Existence[A, B]}: if A occurs, B doesn't (in FrAIm, the same as Not Co-Existence). */
	Not_Responded_Existence("Not Responded Existence", true, true, false),
	/** {@code Not Response[A, B]}: no A is followed by a B. */
	Not_Response("Not Response", true, true, false),
	/** {@code Not Succession[A, B]}: no A is followed by a B (in FrAIm, the same as Not Response). */
	Not_Succession("Not Succession", true, true, false);

	private String templateName;
	private boolean isBinary;
	private boolean isNegative;
	private boolean reverseActivationTarget;

	private String displayText;

	//Reverse-lookup map for getting template object based on template name string
	private static final Map<String, DeclareTemplate> templateNameToTemplate = new HashMap<String, DeclareTemplate>();

	static {
		for (DeclareTemplate constraintTemplate : DeclareTemplate.values()) {
			templateNameToTemplate.put(constraintTemplate.getTemplateName(), constraintTemplate);
		}
	}

	/**
	 * Creates a template.
	 *
	 * @param templateName the template's name in model files
	 * @param isBinary whether the template has two activities (activation and target) rather than one
	 * @param isNegative whether the template forbids something (the "Not ..." templates)
	 * @param reverseActivationTarget whether the activation is the second activity in the model file
	 */
	private DeclareTemplate(String templateName, boolean isBinary, boolean isNegative, boolean reverseActivationTarget) {
		this.templateName = templateName;
		this.isBinary = isBinary;
		this.isNegative = isNegative;
		this.reverseActivationTarget = reverseActivationTarget;

		if (isBinary) {
			this.displayText = templateName + (reverseActivationTarget ? "[B, A]" : "[A, B]");
		} else {
			this.displayText = templateName + "[A]";
		}
	}

	/**
	 * Returns the template's name in model files.
	 *
	 * @return the name, e.g. {@code Not Chain Response}
	 */
	public String getTemplateName() {
		return templateName;
	}

	/**
	 * Tells whether the template has two activities (activation and target) rather than one.
	 *
	 * @return {@code true} for a binary template, e.g. Response
	 */
	public boolean getIsBinary() {
		return isBinary;
	}

	/**
	 * Tells whether the template forbids something (the "Not ..." templates). Not used anywhere in FrAIm.
	 *
	 * @return {@code true} for a negative template
	 */
	public boolean getIsNegative() {
		return isNegative;
	}

	/**
	 * Tells whether the activation is the second activity in the model file, as for Precedence, Alternate Precedence, Chain Precedence,
	 * Not Precedence and Not Chain Precedence.
	 *
	 * @return {@code true} if activation and target are reversed
	 */
	public boolean getReverseActivationTarget() {
		return reverseActivationTarget;
	}

	/**
	 * Returns the template's generic form, with A standing for the activation and B for the target. Not used anywhere in FrAIm.
	 *
	 * @return e.g. {@code Response[A, B]}, {@code Precedence[B, A]} or {@code Init[A]}
	 */
	public String getDisplayText() {
		return displayText;
	}

	/**
	 * Finds a template by its name in model files.
	 *
	 * @param templateName the name, exactly as in model files (case-sensitive), e.g. {@code Not Chain Response}
	 * @return the template, or {@code null} if no template has that name
	 */
	public static DeclareTemplate getByTemplateName(String templateName) {
		return templateNameToTemplate.get(templateName);
	}
}
