package repository;

import db.DBConnection;
import model.Ride;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class RideRepository {
    private RideRepository() {}

    public static List<Ride> getAvailableRides() {
        try (Connection c = DBConnection.get()) {
            /*
             * Lists bookable rides for the public browse/search view.
             * Filters to future rides that are explicitly open and have remaining seats; enriches with driver + vehicle display info.
             */
            String sql =
                    "SELECT r.Ride_ID, r.Origin, r.Destination, r.Departure_Date, r.Seats_Left, r.Status, " +
                    "       CONCAT(u.First_Name, ' ', LEFT(u.Last_Name, 1), '.') AS Driver_Name, " +
                    "       CONCAT(v.Color, ' ', v.Make, ' ', v.Model, ' (Plate ', v.License_Plate, ')') AS Vehicle_Info " +
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
                            rs.getString("Vehicle_Info")
                    ));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("getAvailableRides failed", e);
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
                                null, null
                        ));
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("getRidesForDriver failed", e);
        }
    }

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
                    "JOIN Drivers d ON d.User_ID = u.User_ID " +
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

                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("cancelRide failed", e);
        }
    }
}

