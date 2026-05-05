package repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Centralizes the derived status logic for {@code Rides}.
 *
 * <p>The application stores a {@code Status} column on rides (open/full/completed/cancelled), but portions of that
 * state are derived from time and remaining seats. This helper keeps the derived state synchronized so UI queries
 * and transactional booking logic can reason about ride availability consistently.</p>
 *
 * <p>Callers typically invoke {@link #refreshRideStatuses(Connection)} at the start (and sometimes end) of a
 * transaction that depends on current ride availability.</p>
 */
final class RideStatusStore {
    private RideStatusStore() {}

    /**
     * Refreshes the {@code Rides.Status} column based on time and {@code Seats_Left}.
     *
     * <ul>
     *   <li>Past rides become {@code completed} (unless already {@code cancelled}).</li>
     *   <li>Upcoming rides with no remaining seats become {@code full}.</li>
     *   <li>Upcoming rides reopen to {@code open} when seats become available again.</li>
     * </ul>
     *
     * <p>This method does not manage transactions; it runs using the provided connection and should be called
     * within the caller's transaction boundaries when correctness depends on up-to-date status.</p>
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

