package com.eventproof.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        DatabaseConfig config = DatabaseConfig.load();
        return DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword());
    }
}
