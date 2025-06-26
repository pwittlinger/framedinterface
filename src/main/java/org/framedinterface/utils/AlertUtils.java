package org.framedinterface.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public final class AlertUtils {
	// private constructor to avoid unnecessary instantiation of the class
    private AlertUtils() {
    }
    
    public static void showError(String message) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.getDialogPane().getStylesheets().add("/org/framedinterface/dialogPane.css");
		alert.setContentText(message);
		alert.showAndWait();
	}
    
    public static void showWarning(String message) {
    	Alert alert = new Alert(AlertType.WARNING);
		alert.getDialogPane().getStylesheets().add("/org/framedinterface/dialogPane.css");
    	alert.setContentText(message);
    	alert.showAndWait();
    }
	
    public static void showSuccess(String message) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.getDialogPane().getStylesheets().add("/org/framedinterface/dialogPane.css");
		alert.setContentText(message);
		alert.showAndWait();
	}

}
