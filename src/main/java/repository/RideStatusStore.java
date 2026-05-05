package repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class RideStatusStore {
    private RideStatusStore() {}

    static void refreshRideStatuses(Connection c) throws SQLException {
        // Rides in the past should no longer accept bookings.
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE Rides SET Status = 'completed' WHERE Departure_Date <= NOW() AND Status <> 'completed' AND Status <> 'cancelled'")) {
            ps.executeUpdate();
        }

        // Keep seat-based status aligned for upcoming rides.
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE Rides SET Status = 'full' WHERE Departure_Date > NOW() AND Seats_Left <= 0 AND Status <> 'completed' AND Status <> 'cancelled'")) {
            ps.executeUpdate();
        }

        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE Rides SET Status = 'open' WHERE Departure_Date > NOW() AND Seats_Left > 0 AND Status = 'full'")) {
            ps.executeUpdate();
        }
    }
}

