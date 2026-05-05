package repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Centralizes the derived status logic for rides.
 *
 * The application stores a Status column on rides (open, full, completed, cancelled), but some of that state is
 * derived from time and remaining seats. This helper keeps the derived state synchronized so UI queries and
 * transactional booking logic can reason about availability consistently.
 *
 * Callers typically invoke refreshRideStatuses at the start (and sometimes end) of a transaction that depends on
 * current ride availability.
 */
final class RideStatusStore {
    private RideStatusStore() {}

    /**
     * Refreshes the Rides.Status column based on time and Seats_Left.
     *
     * Rules:
     * - Past rides become completed unless already cancelled.
     * - Upcoming rides with no remaining seats become full.
     * - Upcoming rides reopen to open when seats become available again.
     *
     * This method does not manage transactions; it runs using the provided connection and should be called within
     * the caller's transaction boundaries when correctness depends on up-to-date status.
     *
     * @param c open JDBC connection (may be in a transaction)
     * @throws SQLException if an update fails
     */
    static void refreshRideStatuses(Connection c) throws SQLException {
        // Rides in the past should no longer accept bookings.
        try (PreparedStatement ps = c.prepareStatement(
                /* Auto-complete past rides while preserving explicit cancellations. */
                "UPDATE Rides SET Status = 'completed' WHERE Departure_Date <= NOW() AND Status <> 'completed' AND Status <> 'cancelled'")) {
            ps.executeUpdate();
        }

        /*
         * When rides become completed, finalize accepted bookings and increment passenger ride totals.
         *
         * This is intentionally idempotent:
         * - Only bookings currently in 'accepted' are transitioned to 'completed'
         * - Passenger totals are incremented only for those transitioned rows
         *
         * Doing both changes in one statement ensures totals stay in sync with booking history.
         */
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE Passengers p " +
                "JOIN Bookings b ON b.User_ID = p.User_ID " +
                "JOIN Rides r ON r.Ride_ID = b.Ride_ID " +
                "SET p.Total_Rides_Taken = COALESCE(p.Total_Rides_Taken, 0) + 1, " +
                "    b.Status = 'completed' " +
                "WHERE b.Status = 'accepted' " +
                "  AND r.Status = 'completed' " +
                "  AND r.Departure_Date <= NOW()")) {
            ps.executeUpdate();
        }

        // Keep seat-based status aligned for upcoming rides.
        try (PreparedStatement ps = c.prepareStatement(
                /* Mark upcoming rides full once seats are exhausted (avoid overriding completed/cancelled). */
                "UPDATE Rides SET Status = 'full' WHERE Departure_Date > NOW() AND Seats_Left <= 0 AND Status <> 'completed' AND Status <> 'cancelled'")) {
            ps.executeUpdate();
        }

        try (PreparedStatement ps = c.prepareStatement(
                /* Re-open rides if seats become available again (e.g., cancellations). */
                "UPDATE Rides SET Status = 'open' WHERE Departure_Date > NOW() AND Seats_Left > 0 AND Status = 'full'")) {
            ps.executeUpdate();
        }
    }
}

