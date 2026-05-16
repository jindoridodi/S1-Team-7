package repository;

import db.DBConnection;
import model.Ride;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access layer for ride listings and ride management.
 *
 * Callers rely on RideStatusStore to keep derived ride status consistent before reading or mutating ride state.
 */
public final class RideRepository {
    private RideRepository() {}

    /**
     * Returns rides that can be booked by passengers.
     *
     * @return list of upcoming rides that are open and have remaining seats
     */
    public static List<Ride> getAvailableRides() {
        try (Connection c = DBConnection.get()) {
            /*
             * Lists bookable rides for the public browse/search view.
             * Filters to future rides that are explicitly open and have remaining seats; enriches with driver + vehicle display info.
             */
            String sql =
                    "SELECT r.Ride_ID, r.Origin, r.Destination, r.Departure_Date, r.Seats_Left, r.Status, " +
                    "       CONCAT(u.First_Name, ' ', LEFT(u.Last_Name, 1), '.') AS Driver_Name, " +
                    "       CONCAT(v.Color, ' ', v.Make, ' ', v.Model, ' (Plate ', v.License_Plate, ')') AS Vehicle_Info, " +
                    "       u.Gender AS Driver_Gender " +
                    "FROM Rides r " +
                    "LEFT JOIN Users u ON u.User_ID = r.Driver_ID " +
                    "LEFT JOIN Vehicles v ON v.Vehicle_ID = r.Vehicle_ID " +
                    "WHERE r.Status = 'open' " +
                    "  AND r.Seats_Left > 0 " +
                    "  AND r.Departure_Date > NOW() " +
                    "ORDER BY r.Departure_Date ASC";

            RideStatusStore.refreshRideStatuses(c);

            List<Ride> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Ride(
                            String.valueOf(rs.getInt("Ride_ID")),
                            rs.getString("Origin"),
                            rs.getString("Destination"),
                            rs.getString("Departure_Date"),
                            rs.getInt("Seats_Left"),
                            rs.getString("Status"),
                            rs.getString("Driver_Name"),
                            rs.getString("Vehicle_Info"),
                            rs.getString("Driver_Gender")
                    ));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("getAvailableRides failed", e);
        }
    }

    /**
     * Returns a ride for the passenger seat-request flow, or null if the ride does not exist.
     * Does not filter on open status or remaining seats so passengers can always submit a request.
     */
    public static Ride getRideByIdForSeatRequest(String rideId) {
        try (Connection c = DBConnection.get()) {
            RideStatusStore.refreshRideStatuses(c);
            String sql =
                    "SELECT r.Ride_ID, r.Origin, r.Destination, r.Departure_Date, r.Seats_Left, r.Status, " +
                    "       CONCAT(u.First_Name, ' ', LEFT(u.Last_Name, 1), '.') AS Driver_Name, " +
                    "       CONCAT(v.Color, ' ', v.Make, ' ', v.Model, ' (Plate ', v.License_Plate, ')') AS Vehicle_Info, " +
                    "       u.Gender AS Driver_Gender " +
                    "FROM Rides r " +
                    "LEFT JOIN Users u ON u.User_ID = r.Driver_ID " +
                    "LEFT JOIN Vehicles v ON v.Vehicle_ID = r.Vehicle_ID " +
                    "WHERE r.Ride_ID = ? " +
                    "  AND COALESCE(r.Status, '') NOT IN ('cancelled', 'completed') " +
                    "LIMIT 1";

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(rideId));
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    return new Ride(
                            String.valueOf(rs.getInt("Ride_ID")),
                            rs.getString("Origin"),
                            rs.getString("Destination"),
                            rs.getString("Departure_Date"),
                            rs.getInt("Seats_Left"),
                            rs.getString("Status"),
                            rs.getString("Driver_Name"),
                            rs.getString("Vehicle_Info"),
                            rs.getString("Driver_Gender")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("getRideByIdForSeatRequest failed", e);
        }
    }

    /** Returns all rides created by this driver, most recent first. */
    public static List<Ride> getRidesForDriver(String driverEmail) {
        try (Connection c = DBConnection.get()) {
            /*
             * Lists rides owned by the authenticated driver, guarded by active account status.
             * Used by the driver dashboard; ordering shows newest departures first.
             */
            String sql =
                    "SELECT r.Ride_ID, r.Origin, r.Destination, r.Departure_Date, r.Seats_Left, r.Status " +
                    "FROM Rides r " +
                    "JOIN Users u ON u.User_ID = r.Driver_ID " +
                    "WHERE u.Email = ? AND u.Account_Status = 'active' " +
                    "ORDER BY r.Departure_Date DESC";

            List<Ride> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, driverEmail);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new Ride(
                                String.valueOf(rs.getInt("Ride_ID")),
                                rs.getString("Origin"),
                                rs.getString("Destination"),
                                rs.getString("Departure_Date"),
                                rs.getInt("Seats_Left"),
                                rs.getString("Status"),
                                null, null, null
                        ));
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("getRidesForDriver failed", e);
        }
    }

    /**
     * Rides the driver can still manage: not yet completed/cancelled and departure still in the future
     * (after {@link RideStatusStore#refreshRideStatuses}).
     */
    public static List<Ride> getActiveRidesForDriver(String driverEmail) {
        try (Connection c = DBConnection.get()) {
            RideStatusStore.refreshRideStatuses(c);
            String sql =
                    "SELECT r.Ride_ID, r.Origin, r.Destination, r.Departure_Date, r.Seats_Left, r.Status " +
                    "FROM Rides r " +
                    "JOIN Users u ON u.User_ID = r.Driver_ID " +
                    "WHERE u.Email = ? AND u.Account_Status = 'active' " +
                    "  AND r.Status NOT IN ('completed', 'cancelled') " +
                    "  AND r.Departure_Date >= NOW() " +
                    "ORDER BY r.Departure_Date ASC";

            List<Ride> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, driverEmail);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new Ride(
                                String.valueOf(rs.getInt("Ride_ID")),
                                rs.getString("Origin"),
                                rs.getString("Destination"),
                                rs.getString("Departure_Date"),
                                rs.getInt("Seats_Left"),
                                rs.getString("Status"),
                                null, null, null
                        ));
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("getActiveRidesForDriver failed", e);
        }
    }

    /**
     * Past rides for the driver: completed, cancelled, or departure time in the past.
     */
    public static List<Ride> getRideHistoryForDriver(String driverEmail) {
        try (Connection c = DBConnection.get()) {
            RideStatusStore.refreshRideStatuses(c);
            String sql =
                    "SELECT r.Ride_ID, r.Origin, r.Destination, r.Departure_Date, r.Seats_Left, r.Status, " +
                    "       CONCAT(v.Color, ' ', v.Make, ' ', v.Model, ' (Plate ', v.License_Plate, ')') AS Vehicle_Info " +
                    "FROM Rides r " +
                    "JOIN Users u ON u.User_ID = r.Driver_ID " +
                    "LEFT JOIN Vehicles v ON v.Vehicle_ID = r.Vehicle_ID " +
                    "WHERE u.Email = ? AND u.Account_Status = 'active' " +
                    "  AND (r.Status IN ('completed', 'cancelled') OR r.Departure_Date < NOW()) " +
                    "ORDER BY r.Departure_Date DESC";

            List<Ride> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, driverEmail);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new Ride(
                                String.valueOf(rs.getInt("Ride_ID")),
                                rs.getString("Origin"),
                                rs.getString("Destination"),
                                rs.getString("Departure_Date"),
                                rs.getInt("Seats_Left"),
                                rs.getString("Status"),
                                null,
                                rs.getString("Vehicle_Info"),
                                null
                        ));
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("getRideHistoryForDriver failed", e);
        }
    }

    /**
     * Creates a new ride offer owned by the authenticated driver.
     *
     * @param driverEmail authenticated driver email
     * @param vehicleId vehicle identifier belonging to the driver
     * @param origin ride origin
     * @param destination ride destination
     * @param departureDate departure date/time string as provided by the UI
     * @param seatsLeft number of available seats for booking
     */
    public static void createRide(String driverEmail, String vehicleId, String origin, String destination, String departureDate, int seatsLeft) {
        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            /*
             * Creates a ride offer for an active, verified driver using one of their active vehicles.
             * INSERT ... SELECT enforces ownership (vehicle belongs to driver) and that the user has a Drivers row.
             */
            String insertRide =
                    "INSERT INTO Rides (Driver_ID, Vehicle_ID, Origin, Destination, Departure_Date, Seats_Left, Status) " +
                    "SELECT d.User_ID, v.Vehicle_ID, ?, ?, ?, ?, 'open' " +
                    "FROM Users u " +
                    "JOIN Drivers d ON d.User_ID = u.User_ID AND d.Verification_Status = 'verified' " +
                    "JOIN Vehicles v ON v.Vehicle_ID = ? AND v.Driver_ID = d.User_ID AND v.Vehicle_Status = 'active' " +
                    "WHERE u.Email = ? AND u.Account_Status = 'active'";

            try (PreparedStatement rideStatement = c.prepareStatement(insertRide)) {
                rideStatement.setString(1, origin);
                rideStatement.setString(2, destination);
                rideStatement.setString(3, departureDate);
                rideStatement.setInt(4, seatsLeft);
                rideStatement.setInt(5, Integer.parseInt(vehicleId));
                rideStatement.setString(6, driverEmail);
                if (rideStatement.executeUpdate() == 0) {
                    throw new SQLException("No driver row found for ride assignment");
                }
                LogRepository.log("RIDE_CREATED", null, Integer.parseInt(vehicleId), null, "Ride created by " + driverEmail + " from " + origin + " to " + destination);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("createRide failed", e);
        }
    }

    /**
     * Cancels a ride and notifies all affected passengers via Notifications + Personalizes.
     * Returns true on success, false if ride can't be cancelled.
     */
    public static boolean cancelRide(String driverEmail, String rideId) {
        /*
         * Cancels an owned ride and cascades user-visible effects:
         * - marks active/pending bookings cancelled
         * - marks the ride cancelled
         * - inserts notifications for all affected passengers
         *
         * Uses row locking on the ride to prevent concurrent state changes.
         */
        String verifySQL =
                "SELECT r.Status, r.Origin, r.Destination " +
                "FROM Rides r " +
                "JOIN Users u ON u.User_ID = r.Driver_ID " +
                "WHERE r.Ride_ID = ? AND u.Email = ? AND u.Account_Status = 'active' FOR UPDATE";

        /* Collects passengers with bookings that will be impacted by a cancellation. */
        String getPassengersSQL =
                "SELECT b.User_ID " +
                "FROM Bookings b " +
                "WHERE b.Ride_ID = ? AND b.Status IN ('pending', 'accepted')";

        /* Cancels only bookings that are still actionable (pending/accepted). */
        String cancelBookingsSQL =
                "UPDATE Bookings SET Status = 'cancelled' WHERE Ride_ID = ? AND Status IN ('pending', 'accepted')";

        /* Cancels the ride row itself after ownership verification. */
        String cancelRideSQL =
                "UPDATE Rides SET Status = 'cancelled' WHERE Ride_ID = ?";

        /* Inserts a timestamped notification for each affected passenger. */
        String insertNotifSQL =
                "INSERT INTO Notifications (User_ID, Content, Timestamp) VALUES (?, ?, NOW())";

        boolean cancelled = false;
        String logDescription = null;

        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            try {
                // Verify ride belongs to driver and is cancellable.
                String rideStatus, origin, destination;
                try (PreparedStatement ps = c.prepareStatement(verifySQL)) {
                    ps.setInt(1, Integer.parseInt(rideId));
                    ps.setString(2, driverEmail);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) { c.rollback(); return false; }
                        rideStatus  = rs.getString("Status");
                        origin      = rs.getString("Origin");
                        destination = rs.getString("Destination");
                    }
                }

                if ("cancelled".equalsIgnoreCase(rideStatus) || "completed".equalsIgnoreCase(rideStatus)) {
                    c.rollback();
                    return false;
                }

                // Collect affected passenger IDs before cancelling.
                List<Integer> passengerIds = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement(getPassengersSQL)) {
                    ps.setInt(1, Integer.parseInt(rideId));
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) passengerIds.add(rs.getInt("User_ID"));
                    }
                }

                // Cancel all active bookings.
                try (PreparedStatement ps = c.prepareStatement(cancelBookingsSQL)) {
                    ps.setInt(1, Integer.parseInt(rideId));
                    ps.executeUpdate();
                }

                // Cancel the ride.
                try (PreparedStatement ps = c.prepareStatement(cancelRideSQL)) {
                    ps.setInt(1, Integer.parseInt(rideId));
                    ps.executeUpdate();
                }

                // Notify each affected passenger.
                if (!passengerIds.isEmpty()) {
                    String message = "Your ride from " + origin + " to " + destination + " has been cancelled by the driver.";
                    try (PreparedStatement psNotif = c.prepareStatement(insertNotifSQL)) {
                        for (int userId : passengerIds) {
                            psNotif.setInt(1, userId);
                            psNotif.setString(2, message);
                            psNotif.executeUpdate();
                        }
                    }
                }
                logDescription = "Ride " + rideId + " cancelled by " + driverEmail;
                c.commit();
                cancelled = true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("cancelRide failed", e);
        }

        if (cancelled) {
            LogRepository.log("RIDE_CANCELLED", null, Integer.parseInt(rideId), null, logDescription);
        }

        return cancelled;
    }

    /**
     * Admin cancellation: completes any pending bookings and marks the ride cancelled.
     * Refreshes ride statuses first so time-derived {@code completed} rides cannot be reopened.
     */
    public static boolean cancelRideAsAdmin(String rideId) {
        String verifySQL =
                "SELECT r.Status, r.Origin, r.Destination " +
                "FROM Rides r " +
                "WHERE r.Ride_ID = ? FOR UPDATE";

        String getPassengersSQL =
                "SELECT b.User_ID " +
                "FROM Bookings b " +
                "WHERE b.Ride_ID = ? AND b.Status IN ('pending', 'accepted')";

        String cancelBookingsSQL =
                "UPDATE Bookings SET Status = 'cancelled' WHERE Ride_ID = ? AND Status IN ('pending', 'accepted')";

        String cancelRideSQL =
                "UPDATE Rides SET Status = 'cancelled' WHERE Ride_ID = ?";

        String insertNotifSQL =
                "INSERT INTO Notifications (User_ID, Content, Timestamp) VALUES (?, ?, NOW())";

        boolean cancelled = false;
        String logDescription = null;

        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            try {
                RideStatusStore.refreshRideStatuses(c);

                String rideStatus;
                String origin;
                String destination;
                try (PreparedStatement ps = c.prepareStatement(verifySQL)) {
                    ps.setInt(1, Integer.parseInt(rideId));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            c.rollback();
                            return false;
                        }
                        rideStatus = rs.getString("Status");
                        origin = rs.getString("Origin");
                        destination = rs.getString("Destination");
                    }
                }

                if ("cancelled".equalsIgnoreCase(rideStatus) || "completed".equalsIgnoreCase(rideStatus)) {
                    c.rollback();
                    return false;
                }

                List<Integer> passengerIds = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement(getPassengersSQL)) {
                    ps.setInt(1, Integer.parseInt(rideId));
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            passengerIds.add(rs.getInt("User_ID"));
                        }
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(cancelBookingsSQL)) {
                    ps.setInt(1, Integer.parseInt(rideId));
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = c.prepareStatement(cancelRideSQL)) {
                    ps.setInt(1, Integer.parseInt(rideId));
                    ps.executeUpdate();
                }

                if (!passengerIds.isEmpty()) {
                    String message = "Your ride from " + origin + " to " + destination
                            + " has been cancelled by UniRide administration.";
                    try (PreparedStatement psNotif = c.prepareStatement(insertNotifSQL)) {
                        for (int userId : passengerIds) {
                            psNotif.setInt(1, userId);
                            psNotif.setString(2, message);
                            psNotif.executeUpdate();
                        }
                    }
                }
                logDescription = "Ride " + rideId + " cancelled by admin";
                c.commit();
                cancelled = true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("cancelRideAsAdmin failed", e);
        }

        if (cancelled) {
            LogRepository.log("RIDE_CANCELLED", null, Integer.parseInt(rideId), null, logDescription);
        }

        return cancelled;
    }
}

