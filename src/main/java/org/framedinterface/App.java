package org.framedinterface;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.util.Locale;

import org.framedinterface.controller.InitialController;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/framedinterface/maininterface.fxml"));
		Parent parent = fxmlLoader.load();
        ((InitialController)fxmlLoader.getController()).setStage(stage);
        Locale.setDefault(Locale.UK);
        scene = new Scene(parent);
        //scene.getStylesheets().add(getClass().getResource("/org/framedinterface/main.css").toString());
        stage.setScene(scene);
        stage.setTitle("Framed-Autonomy Planner");
        stage.getIcons().add(new Image(App.class.getResourceAsStream("/org/framedinterface/frAIm.png")));
        
        stage.show();

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent t) {
				Platform.exit();
				System.exit(0);
			}
		});

        
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

}