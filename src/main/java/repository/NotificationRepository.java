package repository;

import db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access layer for user notifications.
 *
 * Methods enforce ownership by joining through the authenticated user's email.
 */
public final class NotificationRepository {
    private NotificationRepository() {}

    /**
     * Inserts an unread notification for a user within an existing transaction.
     */
    public static void insertNotification(Connection connection, int userId, String content) throws SQLException {
        String sql = "INSERT INTO Notifications (User_ID, Content, Timestamp) VALUES (?, ?, NOW())";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, content);
            ps.executeUpdate();
        }
    }

    /** Returns all notifications for a user, most recent first. */
    public static List<String[]> getNotificationsForUser(String email) {
        try (Connection c = DBConnection.get()) {
            /*
             * Fetches notifications for the authenticated user only.
             * Guards by active account and orders newest-first for inbox display.
             */
            String sql =
                    "SELECT n.Notification_ID, n.Content, n.Timestamp, n.Read_Status " +
                    "FROM Notifications n " +
                    "JOIN Users u ON u.User_ID = n.User_ID " +
                    "WHERE u.Email = ? AND u.Account_Status = 'active' " +
                    "ORDER BY n.Timestamp DESC";

            List<String[]> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String status = rs.getString("Read_Status");
                        if (status == null || status.isBlank()) status = "unread";
                        list.add(new String[]{
                                rs.getString("Notification_ID"),
                                rs.getString("Content"),
                                rs.getString("Timestamp"),
                                status
                        });
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("getNotificationsForUser failed", e);
        }
    }

    /** Marks a notification as read. */
    public static void markNotificationRead(String email, String notificationId) {
        /*
         * Marks a single notification read, but only if it belongs to the authenticated active account.
         * This prevents users from toggling read-state for other users' notifications by ID.
         */
        String sql =
                "UPDATE Notifications n " +
                "JOIN Users u ON u.User_ID = n.User_ID " +
                "SET n.Read_Status = 'read' " +
                "WHERE n.Notification_ID = ? AND u.Email = ? AND u.Account_Status = 'active'";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(notificationId));
            ps.setString(2, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("markNotificationRead failed", e);
        }
    }
}

