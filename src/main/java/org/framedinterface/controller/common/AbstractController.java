package org.framedinterface.controller.common;

import javafx.fxml.FXML;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public abstract class AbstractController {

	@FXML
	private Region rootRegion;

	private Stage stage;

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public Region getRootRegion() {
		return rootRegion;
	}

}
