package org.framedinterface.utils;

import java.util.function.UnaryOperator;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;

/**
 * Input validation for text fields, used for the zoom percentage fields next to the model viewers' zoom sliders.
 */
public class ValidationUtils {

	/**
	 * Makes the given text fields only accept a percentage with at most three digits before the decimal point and one after it (e.g.
	 * {@code 100}, {@code 99.5} or {@code .5}), and fill in a valid value when they lose focus: the entered value in the given format, or
	 * {@code upperBound} if the value is larger or isn't a number (e.g. an empty field). Values aren't checked against a lower bound.
	 * <p>
	 * The value is formatted with the default locale's decimal separator, while the fields only accept a {@code .}. FrAIm sets the
	 * default locale to {@link java.util.Locale#UK} at startup, but only after the first page has been loaded.
	 *
	 * @param precentageFormat the format of the value, e.g. {@code %.1f}
	 * @param upperBound the largest value allowed, e.g. 400 for a zoom slider that goes up to 4
	 * @param textFields the fields
	 */
	public static void addMandatoryPrecentageBehavior(String precentageFormat, double upperBound, TextField...textFields) {
		for (TextField textField : textFields) {
			UnaryOperator<Change> filter = new UnaryOperator<TextFormatter.Change>() {
				@Override
				public Change apply(Change change) {
					if (change.getControlNewText().matches("[.][0-9]{0,1}|[0-9]{0,3}|[0-9]{1,3}[.]|[0-9]{1,3}[.][0-9]?")) {
						return change;
					} else {
						return null;
					}
				}
			};
			textField.setTextFormatter(new TextFormatter<Integer>(filter));

			textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
				if (newValue == false) {
					try {
						Double doubleValue = Double.valueOf(textField.getText());
						if (doubleValue <= upperBound) {
							textField.setText(String.format(precentageFormat, doubleValue));
						} else {
							textField.setText(String.format(precentageFormat, upperBound));
						}
					} catch (NumberFormatException e) {
						textField.setText(String.format(precentageFormat, upperBound));
					}
				}
			});
		}
	}

}
