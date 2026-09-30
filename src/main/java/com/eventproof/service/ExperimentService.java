package com.eventproof.service;

import com.eventproof.dao.EventDAO;
import com.eventproof.dao.ExperimentDAO;
import com.eventproof.experiment.ExperimentEngine;
import com.eventproof.model.EventDefinition;
import com.eventproof.model.Experiment;
import com.eventproof.model.ExperimentEvent;
import com.eventproof.model.FaultType;
import com.eventproof.model.GeneratedEvent;

import java.sql.SQLException;
import java.util.List;

public class ExperimentService {
    private final EventDAO eventDAO = new EventDAO();
    private final ExperimentDAO experimentDAO = new ExperimentDAO();
    private final ExperimentEngine experimentEngine = new ExperimentEngine();

    public Experiment runExperiment(long projectId, String experimentName,
                                    FaultType faultType, Long targetEventId) throws SQLException {
        if (projectId <= 0) {
            throw new IllegalArgumentException("Project ID must be positive.");
        }
        if (experimentName == null || experimentName.isBlank()) {
            throw new IllegalArgumentException("Experiment name must not be blank.");
        }
        if (faultType == null) {
            throw new IllegalArgumentException("Fault type must not be null.");
        }

        List<EventDefinition> events = eventDAO.findByProjectId(projectId);
        List<GeneratedEvent> timeline = experimentEngine.generate(events, faultType, targetEventId);
        Experiment experiment = new Experiment(projectId, experimentName, faultType, targetEventId);
        return experimentDAO.saveCompletedExperiment(experiment, timeline);
    }

    public List<Experiment> getProjectExperiments(long projectId) throws SQLException {
        if (projectId <= 0) {
            throw new IllegalArgumentException("Project ID must be positive.");
        }
        return experimentDAO.findByProjectId(projectId);
    }

    public List<ExperimentEvent> getExperimentTimeline(long experimentId) throws SQLException {
        if (experimentId <= 0) {
            throw new IllegalArgumentException("Experiment ID must be positive.");
        }
        return experimentDAO.findTimeline(experimentId);
    }
}
