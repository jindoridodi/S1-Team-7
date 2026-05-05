package repository;

import db.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class AdminRepository {
    private AdminRepository() {}

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