package com.eventproof.service;

import com.eventproof.dao.EventDAO;
import com.eventproof.model.EventDefinition;

import java.sql.SQLException;
import java.util.List;

public class EventService {
    private final EventDAO eventDAO = new EventDAO();

    public EventDefinition createEvent(long projectId, int sequenceNo,
                                       String eventName, String samplePayloadJson) throws SQLException {
        if (projectId <= 0) {
            throw new IllegalArgumentException("Project ID must be positive.");
        }
        if (sequenceNo <= 0) {
            throw new IllegalArgumentException("Sequence number must be positive.");
        }
        if (eventName == null || eventName.isBlank()) {
            throw new IllegalArgumentException("Event name must not be blank.");
        }
        return eventDAO.create(new EventDefinition(projectId, sequenceNo, eventName, samplePayloadJson));
    }

    public List<EventDefinition> getProjectEvents(long projectId) throws SQLException {
        if (projectId <= 0) {
            throw new IllegalArgumentException("Project ID must be positive.");
        }
        return eventDAO.findByProjectId(projectId);
    }
}
