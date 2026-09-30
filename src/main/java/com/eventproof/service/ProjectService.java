package com.eventproof.service;

import com.eventproof.dao.ProjectDAO;
import com.eventproof.model.Project;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ProjectService {
    private final ProjectDAO projectDAO = new ProjectDAO();

    public Project createProject(String name, String description) throws SQLException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name must not be blank.");
        }
        return projectDAO.create(new Project(name, description));
    }

    public List<Project> getAllProjects() throws SQLException {
        return projectDAO.findAll();
    }

    public Optional<Project> getProject(long id) throws SQLException {
        return projectDAO.findById(id);
    }
}
