package org.framedinterface.utils;

import java.util.function.UnaryOperator;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;

public class ValidationUtils {

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
