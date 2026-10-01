package com.eventproof.controller;

import com.eventproof.model.EventDefinition;
import com.eventproof.model.Experiment;
import com.eventproof.model.ExperimentEvent;
import com.eventproof.model.FaultType;
import com.eventproof.model.OccurrenceKind;
import com.eventproof.model.Project;
import com.eventproof.service.EventService;
import com.eventproof.service.ExperimentService;
import com.eventproof.service.ProjectService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExperimentsController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final ProjectService projectService = new ProjectService();
    private final EventService eventService = new EventService();
    private final ExperimentService experimentService = new ExperimentService();
    private final ObservableList<Project> projects = FXCollections.observableArrayList();
    private final ObservableList<EventDefinition> events = FXCollections.observableArrayList();
    private final ObservableList<Experiment> history = FXCollections.observableArrayList();
    private final ObservableList<ExperimentEvent> timeline = FXCollections.observableArrayList();
    private final Map<Long, String> eventNames = new HashMap<>();

    @FXML private TextField experimentNameField;
    @FXML private ComboBox<Project> projectCombo;
    @FXML private ComboBox<FaultType> faultTypeCombo;
    @FXML private ComboBox<EventDefinition> targetEventCombo;
    @FXML private Label formHintLabel;
    @FXML private Label validationLabel;
    @FXML private Button runButton;
    @FXML private VBox resultSummary;
    @FXML private Label resultNameLabel;
    @FXML private Label resultProjectLabel;
    @FXML private Label resultFaultLabel;
    @FXML private Label resultTargetLabel;
    @FXML private Label resultStatusLabel;
    @FXML private TableView<ExperimentEvent> timelineTable;
    @FXML private TableColumn<ExperimentEvent, Integer> executionColumn;
    @FXML private TableColumn<ExperimentEvent, String> timelineEventColumn;
    @FXML private TableColumn<ExperimentEvent, OccurrenceKind> occurrenceColumn;
    @FXML private TableView<Experiment> historyTable;
    @FXML private TableColumn<Experiment, String> historyNameColumn;
    @FXML private TableColumn<Experiment, String> historyFaultColumn;
    @FXML private TableColumn<Experiment, String> historyStatusColumn;
    @FXML private TableColumn<Experiment, String> historyCreatedColumn;
    @FXML private Label historyCountLabel;

    @FXML
    private void initialize() {
        projectCombo.setItems(projects);
        projectCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Project project) {
                return project == null ? "" : project.getName();
            }
            @Override public Project fromString(String text) {
                return null;
            }
        });
        targetEventCombo.setItems(events);
        targetEventCombo.setConverter(new StringConverter<>() {
            @Override public String toString(EventDefinition event) {
                return event == null ? "" : event.getSequenceNo() + "  " + event.getEventName();
            }
            @Override public EventDefinition fromString(String text) {
                return null;
            }
        });
        faultTypeCombo.setItems(FXCollections.observableArrayList(FaultType.values()));

        configureTables();
        projectCombo.valueProperty().addListener((observable, oldValue, project) -> loadProject(project));
        faultTypeCombo.valueProperty().addListener((observable, oldValue, fault) -> updateFaultType());
        historyTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, experiment) -> showExperiment(experiment));
        experimentNameField.textProperty().addListener((observable, oldValue, value) ->
                validationLabel.setText(""));

        faultTypeCombo.setValue(FaultType.NORMAL);
        clearResult();
        loadProjects();
    }

    private void configureTables() {
        timelineTable.setItems(timeline);
        timelineTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        timelineTable.setPlaceholder(new Label("Run or select an experiment to view its timeline."));
        executionColumn.setCellValueFactory(row ->
                new ReadOnlyObjectWrapper<>(row.getValue().getExecutionOrder()));
        timelineEventColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(
                eventNames.getOrDefault(row.getValue().getEventId(), "Unknown event")));
        occurrenceColumn.setCellValueFactory(row ->
                new ReadOnlyObjectWrapper<>(row.getValue().getOccurrenceKind()));
        occurrenceColumn.setCellFactory(column -> new TableCell<>() {
            private final Label chip = new Label();
            @Override
            protected void updateItem(OccurrenceKind kind, boolean empty) {
                super.updateItem(kind, empty);
                if (empty || kind == null) {
                    setGraphic(null);
                    return;
                }
                chip.setText(kind.name());
                chip.getStyleClass().setAll("occurrence-chip",
                        kind == OccurrenceKind.DUPLICATE ? "kind-duplicate" : "kind-normal");
                setGraphic(chip);
            }
        });

        historyTable.setItems(history);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        historyTable.setPlaceholder(new Label("Select a project to view its experiments."));
        historyNameColumn.setCellValueFactory(row ->
                new ReadOnlyStringWrapper(row.getValue().getName()));
        historyFaultColumn.setCellValueFactory(row ->
                new ReadOnlyStringWrapper(row.getValue().getFaultType().name()));
        historyStatusColumn.setCellValueFactory(row ->
                new ReadOnlyStringWrapper(row.getValue().getStatus().name()));
        historyCreatedColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(
                row.getValue().getCreatedAt().atZoneSameInstant(ZoneId.systemDefault()).format(DATE_FORMAT)));
    }

    private void loadProjects() {
        try {
            List<Project> loaded = projectService.getAllProjects();
            projects.setAll(loaded);
            if (loaded.isEmpty()) {
                projectCombo.setPromptText("No projects available");
                formHintLabel.setText("No projects available. Create a project first.");
                runButton.setDisable(true);
            } else {
                runButton.setDisable(false);
                projectCombo.getSelectionModel().selectFirst();
            }
        } catch (SQLException | IllegalStateException exception) {
            formHintLabel.setText("Projects are unavailable. Check the database connection.");
            runButton.setDisable(true);
            showError("Could not load projects",
                    "The project list is unavailable. Check the database connection and try again.");
        }
    }

    private void loadProject(Project project) {
        validationLabel.setText("");
        events.clear();
        eventNames.clear();
        history.clear();
        historyCountLabel.setText("0 RUNS");
        targetEventCombo.getSelectionModel().clearSelection();
        clearResult();
        if (project == null) {
            formHintLabel.setText("No projects available. Create a project first.");
            historyTable.setPlaceholder(new Label("Select a project to view its experiments."));
            updateFaultType();
            return;
        }

        try {
            List<EventDefinition> definitions = eventService.getProjectEvents(project.getId());
            events.setAll(definitions);
            for (EventDefinition event : definitions) {
                eventNames.put(event.getId(), event.getEventName());
            }
            formHintLabel.setText(definitions.isEmpty()
                    ? "No events registered for this project."
                    : "Events run in their registered sequence order.");
            updateFaultType();
            refreshHistory(null);
        } catch (SQLException | IllegalStateException exception) {
            formHintLabel.setText("Could not load this project's events.");
            showError("Could not load project data",
                    "The events or experiment history are unavailable. Check the database connection and try again.");
        }
    }

    private void updateFaultType() {
        FaultType fault = faultTypeCombo.getValue();
        faultTypeCombo.getStyleClass().removeAll("fault-normal", "fault-duplicate", "fault-drop");
        if (fault != null) {
            faultTypeCombo.getStyleClass().add("fault-" + fault.name().toLowerCase());
        }
        boolean needsTarget = fault == FaultType.DUPLICATE || fault == FaultType.DROP;
        if (!needsTarget) {
            targetEventCombo.getSelectionModel().clearSelection();
        }
        targetEventCombo.setDisable(!needsTarget || projectCombo.getValue() == null || events.isEmpty());
        targetEventCombo.setPromptText(!needsTarget ? "Not required for NORMAL"
                : events.isEmpty() ? "No events registered" : "Select a target event");
        validationLabel.setText("");
    }

    @FXML
    private void runExperiment() {
        String name = experimentNameField.getText().trim();
        Project project = projectCombo.getValue();
        FaultType fault = faultTypeCombo.getValue();
        EventDefinition target = targetEventCombo.getValue();
        if (name.isBlank()) {
            validationLabel.setText("Enter an experiment name.");
            return;
        }
        if (project == null) {
            validationLabel.setText("Select a project.");
            return;
        }
        if (fault == null) {
            validationLabel.setText("Select a fault type.");
            return;
        }
        if (events.isEmpty()) {
            validationLabel.setText("No events registered for this project.");
            return;
        }
        if (fault != FaultType.NORMAL && target == null) {
            validationLabel.setText("Select a target event for " + fault.name() + ".");
            return;
        }
        validationLabel.setText("");
        Long targetId = fault == FaultType.NORMAL ? null : target.getId();
        Experiment completed;
        try {
            completed = experimentService.runExperiment(
                    project.getId(), name, fault, targetId);
        } catch (IllegalArgumentException exception) {
            showError("Could not run experiment",
                    "The selected configuration is no longer valid. Reload the project and try again.");
            return;
        } catch (SQLException exception) {
            String message = "22001".equals(exception.getSQLState())
                    ? "The experiment name is too long."
                    : "The experiment could not be saved. Check the database connection and try again.";
            showError("Could not run experiment", message);
            return;
        } catch (IllegalStateException exception) {
            showError("Could not run experiment",
                    "Database configuration is unavailable. Check the local setup and try again.");
            return;
        }
        try {
            refreshHistory(completed.getId());
        } catch (SQLException | IllegalStateException exception) {
            showExperiment(completed);
            showError("Experiment completed",
                    "The experiment was saved, but history could not be refreshed. Reopen this page to retry.");
        }
    }

    private void refreshHistory(Long preferredId) throws SQLException {
        Project project = projectCombo.getValue();
        List<Experiment> loaded = experimentService.getProjectExperiments(project.getId());
        history.setAll(loaded);
        historyCountLabel.setText(loaded.size() + (loaded.size() == 1 ? " RUN" : " RUNS"));
        historyTable.setPlaceholder(new Label("No experiments have been run for this project."));
        Experiment toSelect = null;
        for (Experiment experiment : loaded) {
            if (preferredId != null && experiment.getId() == preferredId) {
                toSelect = experiment;
                break;
            }
        }
        if (toSelect == null && !loaded.isEmpty()) {
            toSelect = loaded.getFirst();
        }
        historyTable.getSelectionModel().select(toSelect);
        if (toSelect == null) {
            clearResult();
        }
    }

    private void showExperiment(Experiment experiment) {
        if (experiment == null) {
            clearResult();
            return;
        }
        try {
            timeline.setAll(experimentService.getExperimentTimeline(experiment.getId()));
            timelineTable.setPlaceholder(new Label("No execution events in this result."));
            resultNameLabel.setText(experiment.getName());
            Project project = projectCombo.getValue();
            resultProjectLabel.setText(project == null ? "Unknown project" : project.getName());
            resultFaultLabel.setText(experiment.getFaultType().name());
            resultFaultLabel.getStyleClass().removeAll("fault-normal", "fault-duplicate", "fault-drop");
            resultFaultLabel.getStyleClass().add(
                    "fault-" + experiment.getFaultType().name().toLowerCase());
            resultTargetLabel.setText(experiment.getTargetEventId() == null ? "—"
                    : eventNames.getOrDefault(experiment.getTargetEventId(), "Unknown event"));
            resultStatusLabel.setText(experiment.getStatus().name());
            resultStatusLabel.getStyleClass().removeAll("status-pending", "status-completed");
            resultStatusLabel.getStyleClass().add(
                    "status-" + experiment.getStatus().name().toLowerCase());
            resultSummary.setVisible(true);
            resultSummary.setManaged(true);
        } catch (SQLException | IllegalStateException exception) {
            clearResult();
            showError("Could not load result",
                    "The saved timeline is unavailable. Check the database connection and try again.");
        }
    }

    private void clearResult() {
        timeline.clear();
        timelineTable.setPlaceholder(new Label("Run or select an experiment to view its timeline."));
        resultSummary.setVisible(false);
        resultSummary.setManaged(false);
    }

    private void showError(String title, String message) {
        if (projectCombo.getScene() == null) {
            Platform.runLater(() -> showError(title, message));
            return;
        }
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("EventProof");
        alert.setHeaderText(title);
        alert.initOwner(projectCombo.getScene().getWindow());
        alert.initModality(Modality.WINDOW_MODAL);
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/eventproof/styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("console-dialog");
        ((Button) alert.getDialogPane().lookupButton(ButtonType.OK))
                .getStyleClass().add("primary-button");
        alert.showAndWait();
    }
}
