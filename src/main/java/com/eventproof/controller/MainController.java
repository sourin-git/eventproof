package com.eventproof.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class MainController {
    @FXML
    private StackPane contentContainer;

    @FXML
    private Button dashboardButton;

    @FXML
    private Button projectsButton;

    @FXML
    private Button experimentsButton;

    @FXML
    private void initialize() {
        showDashboard();
    }

    @FXML
    private void showDashboard() {
        showView("dashboard-view.fxml", dashboardButton);
    }

    @FXML
    private void showProjects() {
        showView("projects-view.fxml", projectsButton);
    }

    @FXML
    private void showExperiments() {
        showView("experiments-view.fxml", experimentsButton);
    }

    private void showView(String fileName, Button selectedButton) {
        String path = "/com/eventproof/" + fileName;
        URL resource = Objects.requireNonNull(getClass().getResource(path), "Missing view: " + path);
        try {
            Parent view = FXMLLoader.load(resource);
            contentContainer.getChildren().setAll(view);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load view: " + path, exception);
        }

        for (Button button : new Button[] {dashboardButton, projectsButton, experimentsButton}) {
            button.getStyleClass().remove("selected");
        }
        selectedButton.getStyleClass().add("selected");
    }
}
