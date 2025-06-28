package org.framedinterface;

import java.util.Locale;

public class AppLauncher {
	
	// helper class to avoids Missing Components exception when running the application
	// Alternative would be to add this to VM options: --module-path /path/to/JavaFX/lib --add-modules=javafx.controls

	public static void main(String[] args) {
		
		Locale.setDefault(Locale.UK); //Avoids issues with decimal conversions to/from Strings
		
		App.main(args); // Starting the application itself
	}
}