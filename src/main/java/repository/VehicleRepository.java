package repository;

import db.DBConnection;
import model.Vehicle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class VehicleRepository {
    private VehicleRepository() {}

    public static List<Vehicle> getVehiclesForOwner(String ownerEmail) {
        String sql =
                "SELECT v.Vehicle_ID, v.Make, v.Model, v.Color, v.License_Plate, v.Total_Seats, v.Insurance_Num " +
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
                            rs.getInt("Total_Seats"),
                            rs.getString("Insurance_Num")
                    ));
                }
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("getVehiclesForOwner failed", e);
        }
    }

    public static void addVehicle(String ownerEmail, String make, String model,
                                  String color, String plate, int totalSeats, String insuranceNum) {
        String insertVehicle =
                "INSERT INTO Vehicles (Driver_ID, License_Plate, Make, Model, Color, Total_Seats, Insurance_Num) " +
                "SELECT u.User_ID, ?, ?, ?, ?, ?, ? FROM Users u " +
                "JOIN Drivers d ON d.User_ID = u.User_ID " +
                "WHERE u.Email = ? AND u.Account_Status = 'active'";

        try (Connection c = DBConnection.get()) {
            try (PreparedStatement pv = c.prepareStatement(insertVehicle)) {
                pv.setString(1, plate);
                pv.setString(2, make);
                pv.setString(3, model);
                pv.setString(4, color);
                pv.setInt(5, totalSeats);
                pv.setString(6, insuranceNum);
                pv.setString(7, ownerEmail);
                if (pv.executeUpdate() == 0) {
                    throw new SQLException("No driver row found for vehicle assignment");
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("addVehicle failed", e);
        }
    }

    public static void updateVehicle(String ownerEmail, String vehicleId, String make, String model,
                                     String color, String plate, int totalSeats, String insuranceNum) {
        String sql =
                "UPDATE Vehicles v " +
                "SET v.Make = ?, v.Model = ?, v.Color = ?, v.License_Plate = ?, v.Total_Seats = ?, v.Insurance_Num = ? " +
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
            ps.setString(6, insuranceNum);
            ps.setInt(7, Integer.parseInt(vehicleId));
            ps.setString(8, ownerEmail);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("updateVehicle failed", e);
        }
    }

    /** Returns true if soft-deleted; false if not owned / already deleted. */
    public static boolean deleteVehicle(String ownerEmail, String vehicleId) {
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

