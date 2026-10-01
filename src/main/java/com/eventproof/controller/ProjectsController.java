package com.eventproof.controller;

import com.eventproof.model.EventDefinition;
import com.eventproof.model.Project;
import com.eventproof.service.EventService;
import com.eventproof.service.ProjectService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProjectsController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final ProjectService projectService = new ProjectService();
    private final EventService eventService = new EventService();
    private final ObservableList<Project> projects = FXCollections.observableArrayList();
    private final ObservableList<EventDefinition> events = FXCollections.observableArrayList();

    @FXML private TableView<Project> projectsTable;
    @FXML private TableColumn<Project, String> projectNameColumn;
    @FXML private TableColumn<Project, String> projectDescriptionColumn;
    @FXML private TableColumn<Project, String> projectCreatedColumn;
    @FXML private Label projectCountLabel;
    @FXML private TableView<EventDefinition> eventsTable;
    @FXML private TableColumn<EventDefinition, Integer> sequenceColumn;
    @FXML private TableColumn<EventDefinition, String> eventNameColumn;
    @FXML private TableColumn<EventDefinition, String> payloadColumn;
    @FXML private Label selectedProjectLabel;
    @FXML private Button addEventButton;

    @FXML
    private void initialize() {
        projectNameColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().getName()));
        projectDescriptionColumn.setCellValueFactory(row ->
                new ReadOnlyStringWrapper(displayText(row.getValue().getDescription())));
        projectCreatedColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(
                row.getValue().getCreatedAt().atZoneSameInstant(ZoneId.systemDefault()).format(DATE_FORMAT)));
        sequenceColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getSequenceNo()));
        eventNameColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().getEventName()));
        payloadColumn.setCellValueFactory(row ->
                new ReadOnlyStringWrapper(displayText(row.getValue().getSamplePayloadJson())));
        payloadColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value);
                setTooltip(empty || value == null || "—".equals(value) ? null : new Tooltip(value));
            }
        });

        projectsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        eventsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        projectsTable.setItems(projects);
        eventsTable.setItems(events);
        projectsTable.setPlaceholder(new Label(
                "No projects yet.\nCreate a project to define an event flow."));
        eventsTable.setPlaceholder(new Label("Select a project to view its events."));
        projectsTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> loadEvents(selected));
        refreshProjects(null);
    }

    private static String displayText(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private void refreshProjects(Long preferredProjectId) {
        try {
            List<Project> loaded = projectService.getAllProjects();
            projects.setAll(loaded);
            projectCountLabel.setText(loaded.size() + (loaded.size() == 1
                    ? " REGISTERED SYSTEM" : " REGISTERED SYSTEMS"));

            Project toSelect = null;
            for (Project project : loaded) {
                if (preferredProjectId != null && project.getId() == preferredProjectId) {
                    toSelect = project;
                    break;
                }
            }
            if (toSelect == null && !loaded.isEmpty()) {
                toSelect = loaded.getFirst();
            }
            projectsTable.getSelectionModel().select(toSelect);
        } catch (SQLException exception) {
            showDatabaseError("Could not load projects",
                    "The project list is unavailable. Check the database connection and try again.");
        }
    }

    private void loadEvents(Project project) {
        events.clear();
        addEventButton.setDisable(project == null);
        if (project == null) {
            selectedProjectLabel.setText("SELECT A PROJECT");
            eventsTable.setPlaceholder(new Label("Select a project to view its events."));
            return;
        }
        selectedProjectLabel.setText(project.getName());
        eventsTable.setPlaceholder(new Label("No events registered for this project."));
        try {
            events.setAll(eventService.getProjectEvents(project.getId()));
        } catch (SQLException exception) {
            eventsTable.setPlaceholder(new Label("Could not load events for this project."));
            showDatabaseError("Could not load events",
                    "The event flow is unavailable. Check the database connection and try again.");
        }
    }

    @FXML
    private void showCreateProjectDialog() {
        Dialog<Project> dialog = new Dialog<>();
        dialog.setTitle("New Project");
        ButtonType createType = new ButtonType("Create Project", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, createType);
        styleDialog(dialog);

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Payment Processing");
        nameField.getStyleClass().add("console-input");
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Optional description");
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(3);
        descriptionArea.getStyleClass().add("console-input");
        Label validation = validationLabel();
        VBox form = new VBox(14,
                field("PROJECT NAME", nameField),
                field("DESCRIPTION", descriptionArea),
                validation);
        form.getStyleClass().add("dialog-form");
        dialog.getDialogPane().setContent(form);

        Project[] created = new Project[1];
        dialog.setResultConverter(button -> button == createType ? created[0] : null);
        Button createButton = (Button) dialog.getDialogPane().lookupButton(createType);
        createButton.getStyleClass().add("primary-button");
        createButton.addEventFilter(ActionEvent.ACTION, event -> {
            String name = nameField.getText().trim();
            String description = descriptionArea.getText().trim();
            if (name.isBlank()) {
                validation.setText("Enter a project name.");
                event.consume();
                return;
            }
            try {
                created[0] = projectService.createProject(
                        name, description.isBlank() ? null : description);
            } catch (SQLException exception) {
                showDatabaseError("Could not create project", projectErrorMessage(exception));
                event.consume();
            }
        });

        dialog.showAndWait().ifPresent(project -> refreshProjects(project.getId()));
    }

    @FXML
    private void showAddEventDialog() {
        Project project = projectsTable.getSelectionModel().getSelectedItem();
        if (project == null) {
            return;
        }

        Dialog<EventDefinition> dialog = new Dialog<>();
        dialog.setTitle("Add Event");
        ButtonType createType = new ButtonType("Add Event", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, createType);
        styleDialog(dialog);

        TextField sequenceField = new TextField();
        sequenceField.setPromptText("e.g. 1");
        sequenceField.getStyleClass().add("console-input");
        TextField eventNameField = new TextField();
        eventNameField.setPromptText("e.g. ORDER_CREATED");
        eventNameField.getStyleClass().add("console-input");
        TextArea payloadArea = new TextArea();
        payloadArea.setPromptText("Optional JSON, e.g. {\"orderId\":\"ORD-1001\"}");
        payloadArea.setWrapText(true);
        payloadArea.setPrefRowCount(4);
        payloadArea.getStyleClass().add("console-input");
        Label validation = validationLabel();
        VBox form = new VBox(14,
                field("SEQUENCE NUMBER", sequenceField),
                field("EVENT NAME", eventNameField),
                field("SAMPLE PAYLOAD JSON", payloadArea),
                validation);
        form.getStyleClass().add("dialog-form");
        dialog.getDialogPane().setContent(form);

        EventDefinition[] created = new EventDefinition[1];
        dialog.setResultConverter(button -> button == createType ? created[0] : null);
        Button createButton = (Button) dialog.getDialogPane().lookupButton(createType);
        createButton.getStyleClass().add("primary-button");
        createButton.addEventFilter(ActionEvent.ACTION, event -> {
            int sequenceNo;
            try {
                sequenceNo = Integer.parseInt(sequenceField.getText().trim());
            } catch (NumberFormatException exception) {
                validation.setText("Enter a positive whole number for the sequence.");
                event.consume();
                return;
            }
            if (sequenceNo <= 0) {
                validation.setText("Sequence number must be greater than zero.");
                event.consume();
                return;
            }
            String eventName = eventNameField.getText().trim();
            if (eventName.isBlank()) {
                validation.setText("Enter an event name.");
                event.consume();
                return;
            }
            String payload = payloadArea.getText().trim();
            try {
                created[0] = eventService.createEvent(project.getId(), sequenceNo, eventName,
                        payload.isBlank() ? null : payload);
            } catch (SQLException exception) {
                showDatabaseError("Could not add event", eventErrorMessage(exception));
                event.consume();
            }
        });

        dialog.showAndWait().ifPresent(createdEvent -> loadEvents(project));
    }

    private VBox field(String title, javafx.scene.Node input) {
        Label label = new Label(title);
        label.getStyleClass().add("field-label");
        return new VBox(7, label, input);
    }

    private Label validationLabel() {
        Label label = new Label();
        label.getStyleClass().add("validation-error");
        label.setWrapText(true);
        return label;
    }

    private void styleDialog(Dialog<?> dialog) {
        dialog.initOwner(projectsTable.getScene().getWindow());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/eventproof/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("console-dialog");
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.getStyleClass().add("secondary-button");
        }
    }

    private void showDatabaseError(String title, String message) {
        if (projectsTable.getScene() == null) {
            Platform.runLater(() -> showDatabaseError(title, message));
            return;
        }
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("EventProof");
        alert.setHeaderText(title);
        styleDialog(alert);
        ((Button) alert.getDialogPane().lookupButton(ButtonType.OK))
                .getStyleClass().add("primary-button");
        alert.showAndWait();
    }

    private String projectErrorMessage(SQLException exception) {
        if ("22001".equals(exception.getSQLState())) {
            return "The project name or description is too long.";
        }
        return "The project could not be saved. Check the database connection and try again.";
    }

    private String eventErrorMessage(SQLException exception) {
        return switch (String.valueOf(exception.getSQLState())) {
            case "23505" -> "This project already has an event at that sequence number.";
            case "22P02" -> "Sample payload must be valid JSON.";
            case "22001" -> "The event name is too long.";
            default -> "The event could not be saved. Check the details and try again.";
        };
    }
}
