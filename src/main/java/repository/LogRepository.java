package repository;

import db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
/**
 * Fire-and-forget audit logging.
 * Errors are swallowed so logging never breaks the main app flow.
 */
public final class LogRepository {
    private LogRepository() {}

    public static void log(String actionType, Integer userId, Integer rideId, Integer bookingId, String description) {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(
                "INSERT INTO Logs (User_ID, Ride_ID, Booking_ID, Action_Type, Description) " +
                "VALUES (?, ?, ?, ?, ?)")) {
            ps.setObject(1, userId);
            ps.setObject(2, rideId);
            ps.setObject(3, bookingId);
            ps.setString(4, actionType);
            ps.setString(5, description);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    /** Convenience method when only user context is available. */
    public static void logUser(String actionType, Integer userId, String description) {
        log(actionType, userId, null, null, description);
    }
}