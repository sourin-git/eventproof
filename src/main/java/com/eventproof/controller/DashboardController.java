package com.eventproof.controller;

import com.eventproof.model.Project;
import com.eventproof.service.EventService;
import com.eventproof.service.ExperimentService;
import com.eventproof.service.ProjectService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.sql.SQLException;
import java.util.List;

public class DashboardController {
    private final ProjectService projectService = new ProjectService();
    private final EventService eventService = new EventService();
    private final ExperimentService experimentService = new ExperimentService();

    @FXML private Label projectsMetric;
    @FXML private Label eventsMetric;
    @FXML private Label experimentsMetric;
    @FXML private Label metricsStatusLabel;

    @FXML
    private void initialize() {
        try {
            List<Project> projects = projectService.getAllProjects();
            long eventCount = 0;
            long experimentCount = 0;
            for (Project project : projects) {
                eventCount += eventService.getProjectEvents(project.getId()).size();
                experimentCount += experimentService.getProjectExperiments(project.getId()).size();
            }
            projectsMetric.setText(Integer.toString(projects.size()));
            eventsMetric.setText(Long.toString(eventCount));
            experimentsMetric.setText(Long.toString(experimentCount));
            metricsStatusLabel.setText("LIVE DATA  /  Return to Dashboard to refresh counts.");
        } catch (SQLException | IllegalStateException exception) {
            projectsMetric.setText("—");
            eventsMetric.setText("—");
            experimentsMetric.setText("—");
            metricsStatusLabel.setText("DATA UNAVAILABLE  /  Check the database connection.");
        }
    }
}
