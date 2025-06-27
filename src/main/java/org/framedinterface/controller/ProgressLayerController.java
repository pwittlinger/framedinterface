package org.framedinterface.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ProgressLayerController {
	@FXML
	private Label progressTextLabel;
	@FXML
	private Button cancelButton;

	//Cancelling logic will be implemented by the calling class and tied to cancelButton
	public Button getCancelButton() {
		return cancelButton;
	}

	//Allows to change the default progress text
	public Label getProgressTextLabel() {
		return progressTextLabel;
	}
}
