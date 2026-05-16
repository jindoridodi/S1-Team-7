package repository;

import db.DBConnection;
import model.Vehicle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access layer for driver vehicle management.
 *
 * Operations are guarded by the authenticated email and active account status, and vehicles are soft-deleted by
 * switching Vehicle_Status rather than removing rows.
 */
public final class VehicleRepository {
    private VehicleRepository() {}

    /**
     * Lists active vehicles for the authenticated driver.
     *
     * @param ownerEmail authenticated driver email
     * @return active vehicles owned by the driver
     */
    public static List<Vehicle> getVehiclesForOwner(String ownerEmail) {
        /*
         * Fetches vehicles owned by the currently authenticated driver.
         * Enforces ownership via Users↔Vehicles join and ignores inactive/deleted accounts or vehicles.
         */
        String sql =
                "SELECT v.Vehicle_ID, v.Make, v.Model, v.Color, v.License_Plate, v.Total_Seats " +
                "FROM Vehicles v " +
                "JOIN Users u ON u.User_ID = v.Driver_ID " +
                "WHERE u.Email = ? AND u.Account_Status = 'active' AND v.Vehicle_Status = 'active'";

        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, ownerEmail);
            List<Vehicle> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                        list.add(new Vehicle(
                            String.valueOf(rs.getInt("Vehicle_ID")),
                            ownerEmail,
                            rs.getString("Make"),
                            rs.getString("Model"),
                            rs.getString("Color"),
                            rs.getString("License_Plate"),
                            rs.getInt("Total_Seats")
                        ));
                }
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("getVehiclesForOwner failed", e);
        }
    }

    /**
     * Adds a new vehicle to the authenticated driver's account.
     *
     * @param ownerEmail authenticated driver email
     * @param make vehicle make
     * @param model vehicle model
     * @param color vehicle color
     * @param plate license plate
      * @param totalSeats total seat capacity
     */
     public static void addVehicle(String ownerEmail, String make, String model,
                                             String color, String plate, int totalSeats) {
        /*
         * Registers a vehicle for an existing active driver account.
         * Uses a SELECT-based INSERT to enforce: user exists + has a Drivers row + account is active.
         */
        String insertVehicle =
                "INSERT INTO Vehicles (Driver_ID, License_Plate, Make, Model, Color, Total_Seats) " +
            "SELECT u.User_ID, ?, ?, ?, ?, ? FROM Users u " +
                "JOIN Drivers d ON d.User_ID = u.User_ID " +
                "WHERE u.Email = ? AND u.Account_Status = 'active'";

        try (Connection c = DBConnection.get()) {
            try (PreparedStatement pv = c.prepareStatement(insertVehicle)) {
                pv.setString(1, plate);
                pv.setString(2, make);
                pv.setString(3, model);
                pv.setString(4, color);
                pv.setInt(5, totalSeats);
                pv.setString(6, ownerEmail);
                if (pv.executeUpdate() == 0) {
                    throw new SQLException("No driver row found for vehicle assignment");
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("addVehicle failed", e);
        }
    }

    /**
     * Updates an existing vehicle only if it is owned by the authenticated driver and still active.
     *
     * @param ownerEmail authenticated driver email
     * @param vehicleId vehicle identifier
     * @param make vehicle make
     * @param model vehicle model
     * @param color vehicle color
     * @param plate license plate
    * @param totalSeats total seat capacity
     */
    public static void updateVehicle(String ownerEmail, String vehicleId, String make, String model,
                              String color, String plate, int totalSeats) {
        /*
         * Updates vehicle details only if the vehicle is active and belongs to the given active user.
         * Ownership is enforced via an EXISTS subquery on Users by email.
         */
        String sql =
                "UPDATE Vehicles v " +
                "SET v.Make = ?, v.Model = ?, v.Color = ?, v.License_Plate = ?, v.Total_Seats = ? " +
                "WHERE v.Vehicle_ID = ? " +
                "AND v.Vehicle_Status = 'active' " +
                "AND EXISTS (SELECT 1 FROM Users u WHERE u.User_ID = v.Driver_ID AND u.Email = ? AND u.Account_Status = 'active')";

        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, make);
            ps.setString(2, model);
            ps.setString(3, color);
            ps.setString(4, plate);
            ps.setInt(5, totalSeats);
            ps.setInt(6, Integer.parseInt(vehicleId));
            ps.setString(7, ownerEmail);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("updateVehicle failed", e);
        }
    }

    /** Returns true if soft-deleted; false if not owned / already deleted. */
    public static boolean deleteVehicle(String ownerEmail, String vehicleId) {
        /*
         * Soft-deletes a vehicle (status -> deleted) but only for the owning active account.
         * Prevents repeat deletes by requiring current Vehicle_Status = 'active'.
         */
        String softDeleteSql =
                "UPDATE Vehicles v " +
                "JOIN Users u ON u.User_ID = v.Driver_ID " +
                "SET v.Vehicle_Status = 'deleted' " +
                "WHERE u.Email = ? AND u.Account_Status = 'active' AND v.Vehicle_ID = ? AND v.Vehicle_Status = 'active'";

        try (Connection c = DBConnection.get()) {
            try (PreparedStatement ps = c.prepareStatement(softDeleteSql)) {
                ps.setString(1, ownerEmail);
                ps.setInt(2, Integer.parseInt(vehicleId));
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("deleteVehicle failed", e);
        }
    }
}

