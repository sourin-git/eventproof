package com.eventproof.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class DatabaseConfig {
    private final String url;
    private final String username;
    private final String password;

    private DatabaseConfig(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public static DatabaseConfig load() {
        try (InputStream input = DatabaseConfig.class.getResourceAsStream("/database.properties")) {
            if (input == null) {
                throw new IllegalStateException(
                        "Missing database.properties. Copy database.properties.example to "
                                + "src/main/resources/database.properties and set your local credentials."
                );
            }

            Properties properties = new Properties();
            properties.load(input);
            return new DatabaseConfig(
                    required(properties, "db.url").trim(),
                    required(properties, "db.user").trim(),
                    required(properties, "db.password")
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read database.properties.", exception);
        }
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing or blank " + key + " in database.properties.");
        }
        return value;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
