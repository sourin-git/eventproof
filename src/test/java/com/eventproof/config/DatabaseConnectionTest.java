package com.eventproof.config;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConnectionTest {

    @Test
    void selectOneReturnsOne() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1");
             ResultSet result = statement.executeQuery()) {
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
    }
}
