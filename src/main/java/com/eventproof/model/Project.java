package com.eventproof.model;

import java.time.OffsetDateTime;

public class Project {
    private final long id;
    private final String name;
    private final String description;
    private final OffsetDateTime createdAt;

    public Project(String name, String description) {
        this(0, name, description, null);
    }

    public Project(long id, String name, String description, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Project{id=" + id + ", name='" + name + "', description='"
                + description + "', createdAt=" + createdAt + "}";
    }
}
