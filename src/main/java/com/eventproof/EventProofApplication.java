package com.eventproof;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class EventProofApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eventproof/main-view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 720, 480);
        scene.getStylesheets().add(getClass().getResource("/com/eventproof/styles.css").toExternalForm());

        stage.setTitle("EventProof");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
