package repository;

import db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class AdminRepository {
    private AdminRepository() {}

    /**
     * All rides (after status refresh), newest departure first, with host identity for the admin console.
     *
     * Each row: rideId, driverFirstName, driverLastName, driverEmail, origin, destination, departureDate, seatsLeft,
     * status.
     */
    public static List<String[]> getAllRidesForAdmin() {
        String sql =
                "SELECT r.Ride_ID, u.First_Name, u.Last_Name, u.Email, r.Origin, r.Destination, " +
                "       r.Departure_Date, r.Seats_Left, r.Status " +
                "FROM Rides r " +
                "JOIN Users u ON u.User_ID = r.Driver_ID " +
                "ORDER BY r.Departure_Date DESC";

        try (Connection c = DBConnection.get()) {
            RideStatusStore.refreshRideStatuses(c);
            List<String[]> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new String[]{
                            rs.getString("Ride_ID"),
                            rs.getString("First_Name"),
                            rs.getString("Last_Name"),
                            rs.getString("Email"),
                            rs.getString("Origin"),
                            rs.getString("Destination"),
                            rs.getString("Departure_Date"),
                            rs.getString("Seats_Left"),
                            rs.getString("Status")
                    });
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("getAllRidesForAdmin failed", e);
        }
    }

    /** Returns all pending drivers with their user info. */
    public static List<String[]> getPendingDrivers() {
        String sql =
            "SELECT u.User_ID, u.First_Name, u.Last_Name, u.Email, u.SJSU_ID, d.License_Number " +
            "FROM Users u " +
            "JOIN Drivers d ON d.User_ID = u.User_ID " +
            "WHERE d.Verification_Status = 'pending' AND u.Account_Status = 'active' " +
            "ORDER BY u.User_ID ASC";

        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            List<String[]> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new String[]{
                        rs.getString("User_ID"),
                        rs.getString("First_Name"),
                        rs.getString("Last_Name"),
                        rs.getString("Email"),
                        rs.getString("SJSU_ID"),
                        rs.getString("License_Number")
                    });
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("getPendingDrivers failed", e);
        }
    }

    /** Verifies a driver by setting their status to verified. */
    public static boolean verifyDriver(String userId) {
        String sql = "UPDATE Drivers SET Verification_Status = 'verified' WHERE User_ID = ?";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(userId));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("verifyDriver failed", e);
        }
    }

    /** Rejects a driver by setting their status to rejected. */
    public static boolean rejectDriver(String userId) {
        String sql = "UPDATE Drivers SET Verification_Status = 'rejected' WHERE User_ID = ?";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(userId));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("rejectDriver failed", e);
        }
    }
}