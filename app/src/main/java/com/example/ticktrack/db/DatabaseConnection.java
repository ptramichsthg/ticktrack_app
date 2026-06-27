package com.example.ticktrack.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://10.0.2.2:3306/ticktrack";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() {
        Connection connection = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            DriverManager.setLoginTimeout(5); // 5 seconds timeout
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public static void setupDatabase() {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String[] queries = {
                    "CREATE TABLE IF NOT EXISTS notifications (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "user_id INT, " +
                        "title VARCHAR(255), " +
                        "message TEXT, " +
                        "is_read BOOLEAN DEFAULT FALSE, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
                        
                    "CREATE TABLE IF NOT EXISTS attachments (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "ticket_id INT, " +
                        "reply_id INT NULL, " +
                        "file_name VARCHAR(255), " +
                        "mime_type VARCHAR(50), " +
                        "file_data LONGBLOB, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
                        
                    "CREATE TABLE IF NOT EXISTS activities (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "ticket_id INT, " +
                        "user_id INT, " +
                        "action_type VARCHAR(100), " +
                        "description TEXT, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
                };
                
                for (String q : queries) {
                    try (java.sql.Statement stmt = conn.createStatement()) {
                        stmt.execute(q);
                    } catch (SQLException ignored) {}
                }
                
                try (java.sql.Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE users ADD COLUMN avatar LONGBLOB NULL");
                } catch (SQLException ignored) {}
                
                try (java.sql.Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE users ADD COLUMN is_active TINYINT(1) DEFAULT 1");
                } catch (SQLException ignored) {}

                try (java.sql.Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE categories ADD COLUMN color VARCHAR(7) DEFAULT '#3B82F6'");
                } catch (SQLException ignored) {}
                
                try (java.sql.Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE notifications ADD COLUMN ticket_id INT NULL");
                } catch (SQLException ignored) {}
            }
        } catch (SQLException ignored) {}
    }
}
