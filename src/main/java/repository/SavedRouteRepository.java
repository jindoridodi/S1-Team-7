package repository;

import db.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class SavedRouteRepository {
    private SavedRouteRepository() {}

    /** Returns all saved routes for a user. */
    public static List<String[]> getRoutesForUser(String email) {
        String sql =
            "SELECT sr.Route_ID, sr.Start_Location, sr.End_Location, sr.Frequency " +
            "FROM Saved_Routes sr " +
            "JOIN Users u ON u.User_ID = sr.User_ID " +
            "WHERE u.Email = ? AND u.Account_Status = 'active' " +
            "ORDER BY sr.Route_ID ASC";

        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            List<String[]> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new String[]{
                        rs.getString("Route_ID"),
                        rs.getString("Start_Location"),
                        rs.getString("End_Location"),
                        rs.getString("Frequency")
                    });
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("getRoutesForUser failed", e);
        }
    }

    /** Saves a new route for the user. Ignores duplicates due to unique constraint. */
    public static void saveRoute(String email, String startLocation, String endLocation) {
        String sql =
            "INSERT IGNORE INTO Saved_Routes (User_ID, Start_Location, End_Location, Frequency) " +
            "SELECT u.User_ID, ?, ?, 'occasional' FROM Users u " +
            "WHERE u.Email = ? AND u.Account_Status = 'active'";

        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, startLocation);
            ps.setString(2, endLocation);
            ps.setString(3, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("saveRoute failed", e);
        }
    }

    /** Deletes a saved route belonging to the user. */
    public static void deleteRoute(String email, String routeId) {
        String sql =
            "DELETE sr FROM Saved_Routes sr " +
            "JOIN Users u ON u.User_ID = sr.User_ID " +
            "WHERE sr.Route_ID = ? AND u.Email = ? AND u.Account_Status = 'active'";

        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(routeId));
            ps.setString(2, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("deleteRoute failed", e);
        }
    }
}