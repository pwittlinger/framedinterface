package org.framedinterface.util;

public enum PageType {
	DATA_AGNOSTIC("pages/dataagnostic/DataAgnosticPage.fxml"),
	DATA_AWARE("pages/dataaware/DataAwarePage.fxml"),
	PROCESS_FRAME_OVERVIEW("pages/processframeoverview/ProcessFrameOverviewPage.fxml");

	private final String pathToFxml;

	private PageType(String pathToFxml) {
		this.pathToFxml = pathToFxml;
	}

	public String getPathToFxml() {
		return pathToFxml;
	}
}
