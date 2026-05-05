package store;

import db.DBConnection;
import model.PassengerRequest;
import model.User;
import model.Ride;
import model.UpcomingRide;
import model.Vehicle;
import at.favre.lib.crypto.bcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AppStore {

    private AppStore() {}

    private static final int BCRYPT_COST = 12;

    /**
     * Enforces the application's password policy.
     * Passwords must be at least 8 characters and include at least one
     * uppercase letter, one lowercase letter, and one digit.
     *
     * @param password candidate password
     * @return true when the password meets the policy
     */
    public static boolean isStrongPassword(String password) {
        if (password == null) return false;
        if (password.length() < 8) return false;
        boolean hasUpper = false, hasLower = false, hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) hasUpper = true;
            if (Character.isLowerCase(c)) hasLower = true;
            if (Character.isDigit(c)) hasDigit = true;
            if (hasUpper && hasLower && hasDigit) return true;
        }
        return false;
    }
    private static String hashPassword(String password) {
        if (password == null) throw new IllegalArgumentException("password is required");
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray());
    }

    private static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null) return false;
        return BCrypt.verifyer().verify(password.toCharArray(), storedHash).verified;
    }

    private static void refreshRideStatuses(Connection c) throws SQLException {
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

    public static boolean hasUser(String email) {
        String sql = "SELECT 1 FROM Users WHERE Email = ? AND Account_Status = 'active' LIMIT 1";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("hasUser failed", e);
        }
    }

    public static User createUser(
            String firstName,
            String lastName,
            String email,
            String sjsuId,
            String gender,
            String password,
            Set<String> roles,
            String licenseNumber) {

        if (hasUser(email)) return null;
        if (!isStrongPassword(password)) throw new IllegalArgumentException("password does not meet policy");

        String insertUser =
            "INSERT INTO Users (SJSU_ID, First_Name, Last_Name, Email, Gender, " +
            "                   Password_Hash, Account_Status) " +
            "VALUES (?, ?, ?, ?, ?, ?, 'active')";

        try (Connection c = DBConnection.get()) {
            String passwordHash = hashPassword(password);

            int userId;
            try (PreparedStatement ps = c.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, sjsuId);
                ps.setString(2, firstName);
                ps.setString(3, lastName);
                ps.setString(4, email);
                ps.setString(5, gender);
                ps.setString(6, passwordHash);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No generated key returned");
                    userId = keys.getInt(1);
                }
            }

            if (roles.contains("driver")) {
                try (PreparedStatement pd = c.prepareStatement(
                        "INSERT INTO Drivers (User_ID, License_Number, Verification_Status, Driver_Rating) VALUES (?, ?, 'pending', 0.0)")) {
                    pd.setInt(1, userId);
                    pd.setString(2, licenseNumber);
                    pd.executeUpdate();
                }
            }
            if (roles.contains("passenger")) {
                try (PreparedStatement pp = c.prepareStatement(
                        "INSERT INTO Passengers (User_ID, Total_Rides_Taken) VALUES (?, 0)")) {
                    pp.setInt(1, userId);
                    pp.executeUpdate();
                }
            }

            return new User(firstName, lastName, email, sjsuId, gender, passwordHash, roles);

        } catch (SQLException e) {
            throw new RuntimeException("createUser failed", e);
        }
    }

    public static User authenticate(String email, String password) {
        String sql =
            "SELECT u.First_Name, u.Last_Name, u.SJSU_ID, u.Gender, u.Password_Hash, " +
            "       CASE WHEN d.User_ID IS NOT NULL THEN 1 ELSE 0 END AS Is_Driver, " +
            "       CASE WHEN p.User_ID IS NOT NULL THEN 1 ELSE 0 END AS Is_Passenger " +
            "FROM Users u " +
            "LEFT JOIN Drivers d ON d.User_ID = u.User_ID " +
            "LEFT JOIN Passengers p ON p.User_ID = u.User_ID " +
            "WHERE u.Email = ? AND u.Account_Status = 'active' LIMIT 1";

        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                String storedHash = rs.getString("Password_Hash");
                if (!verifyPassword(password, storedHash)) return null;

                String firstName    = rs.getString("First_Name");
                String lastName     = rs.getString("Last_Name");
                String sjsuId       = rs.getString("SJSU_ID");
                String gender       = rs.getString("Gender");
                boolean isDriver    = rs.getInt("Is_Driver") == 1;
                boolean isPassenger = rs.getInt("Is_Passenger") == 1;

                Set<String> roles = new HashSet<>();
                if (isDriver)    roles.add("driver");
                if (isPassenger) roles.add("passenger");

                return new User(firstName, lastName, email, sjsuId, gender, storedHash, roles);
            }
        } catch (SQLException e) {
            throw new RuntimeException("authenticate failed", e);
        }
    }

    public static void deleteUser(String email) {
        String sql = "UPDATE Users SET Account_Status = 'deleted' WHERE Email = ?";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("deleteUser failed", e);
        }
    }

    public static boolean updatePassword(String email, String newPassword) {
        if (!isStrongPassword(newPassword)) return false;
        String sql = "UPDATE Users SET Password_Hash = ? WHERE Email = ? AND Account_Status = 'active'";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, hashPassword(newPassword));
            ps.setString(2, email);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("updatePassword failed", e);
        }
    }

    public static String getDriverVerificationStatus(String email) {
        String sql =
            "SELECT d.Verification_Status FROM Drivers d " +
            "JOIN Users u ON u.User_ID = d.User_ID " +
            "WHERE u.Email = ? AND u.Account_Status = 'active' LIMIT 1";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("Verification_Status") : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("getDriverVerificationStatus failed", e);
        }
    }

    // --------------------------------------------------------------- vehicles

    public static List<Vehicle> getVehiclesForOwner(String ownerEmail) {
        String sql =
            "SELECT v.Vehicle_ID, v.Make, v.Model, v.Color, v.License_Plate, v.Total_Seats, v.Insurance_Num " +
            "FROM Vehicles v " +
            "JOIN Users u ON u.User_ID = v.Driver_ID " +
            "WHERE u.Email = ? AND u.Account_Status = 'active'";

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

    public static void deleteVehicle(String ownerEmail, String vehicleId) {
        String sql =
            "DELETE v FROM Vehicles v " +
            "JOIN Users u ON u.User_ID = v.Driver_ID " +
            "WHERE u.Email = ? AND u.Account_Status = 'active' AND v.Vehicle_ID = ?";

        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ownerEmail);
            ps.setInt(2, Integer.parseInt(vehicleId));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("deleteVehicle failed", e);
        }
    }

    // --------------------------------------------------------------- rides

    public static void createBooking(String passengerEmail, String origin, String destination, String departureDate, int seatsLeft) {
        String bookingInsertSql =
            "INSERT INTO Bookings (User_ID, Ride_ID, Origin, Destination, Departure_Date, Seats_Requested, Booking_Timestamp, Status) " +
            "VALUES ((SELECT User_ID FROM Users WHERE Email = ? AND Account_Status = 'active'), NULL, ?, ?, ?, ?, CURRENT_TIMESTAMP, 'pending')";

        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(bookingInsertSql)) {
            ps.setString(1, passengerEmail);
            ps.setString(2, origin);
            ps.setString(3, destination);
            ps.setString(4, departureDate);
            ps.setInt(5, seatsLeft); // seatsLeft param represents seats requested from the form
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No passenger row found for booking request");
            }
        } catch (SQLException e) {
            throw new RuntimeException("createBooking failed", e);
        }
    }

    public static java.util.List<Ride> getAvailableRides() {
        try (Connection c = DBConnection.get()) {
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

            refreshRideStatuses(c);

            java.util.List<Ride> list = new java.util.ArrayList<>();
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

    public static java.util.List<UpcomingRide> getUpcomingRidesForPassenger(String passengerEmail) {
        try (Connection c = DBConnection.get()) {
            refreshRideStatuses(c);

            String sql =
                "SELECT b.Booking_ID, b.Status AS Booking_Status, " +
                "       COALESCE(r.Origin, b.Origin) AS Origin, " +
                "       COALESCE(r.Destination, b.Destination) AS Destination, " +
                "       COALESCE(r.Departure_Date, b.Departure_Date) AS Departure_Date, " +
                "       CASE WHEN b.Ride_ID IS NULL THEN b.Seats_Requested ELSE r.Seats_Left END AS Seats_Left, " +
                "       CONCAT(du.First_Name, ' ', LEFT(du.Last_Name, 1), '.') AS Driver_Name, " +
                "       CONCAT(v.Color, ' ', v.Make, ' ', v.Model, ' (Plate ', v.License_Plate, ')') AS Vehicle_Info " +
                "FROM Bookings b " +
                "JOIN Users pu ON pu.User_ID = b.User_ID " +
                "LEFT JOIN Rides r ON r.Ride_ID = b.Ride_ID " +
                "LEFT JOIN Users du ON du.User_ID = r.Driver_ID " +
                "LEFT JOIN Vehicles v ON v.Vehicle_ID = r.Vehicle_ID " +
                "WHERE pu.Email = ? " +
                "  AND pu.Account_Status = 'active' " +
                "  AND b.Status IN ('pending', 'accepted') " +
                "  AND COALESCE(r.Departure_Date, b.Departure_Date) >= NOW() " +
                "ORDER BY COALESCE(r.Departure_Date, b.Departure_Date) ASC";

            try (PreparedStatement ps2 = c.prepareStatement(sql)) {
                ps2.setString(1, passengerEmail);
                java.util.List<UpcomingRide> list = new java.util.ArrayList<>();
                try (ResultSet rs = ps2.executeQuery()) {
                    while (rs.next()) {
                        list.add(new UpcomingRide(
                            String.valueOf(rs.getInt("Booking_ID")),
                            rs.getString("Booking_Status"),
                            rs.getString("Origin"),
                            rs.getString("Destination"),
                            rs.getString("Departure_Date"),
                            rs.getInt("Seats_Left"),
                            rs.getString("Driver_Name"),
                            rs.getString("Vehicle_Info")
                        ));
                    }
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("getUpcomingRidesForPassenger failed", e);
        }
    }

    public static java.util.List<UpcomingRide> getRideHistoryForPassenger(String passengerEmail) {
        try (Connection c = DBConnection.get()) {
            refreshRideStatuses(c);

            String sql =
                "SELECT b.Booking_ID, b.Status AS Booking_Status, " +
                "       COALESCE(r.Origin, b.Origin) AS Origin, " +
                "       COALESCE(r.Destination, b.Destination) AS Destination, " +
                "       COALESCE(r.Departure_Date, b.Departure_Date) AS Departure_Date, " +
                "       CASE WHEN b.Ride_ID IS NULL THEN b.Seats_Requested ELSE r.Seats_Left END AS Seats_Left, " +
                "       CONCAT(du.First_Name, ' ', LEFT(du.Last_Name, 1), '.') AS Driver_Name, " +
                "       CONCAT(v.Color, ' ', v.Make, ' ', v.Model, ' (Plate ', v.License_Plate, ')') AS Vehicle_Info " +
                "FROM Bookings b " +
                "JOIN Users pu ON pu.User_ID = b.User_ID " +
                "LEFT JOIN Rides r ON r.Ride_ID = b.Ride_ID " +
                "LEFT JOIN Users du ON du.User_ID = r.Driver_ID " +
                "LEFT JOIN Vehicles v ON v.Vehicle_ID = r.Vehicle_ID " +
                "WHERE pu.Email = ? " +
                "  AND pu.Account_Status = 'active' " +
                "  AND (b.Status IN ('cancelled', 'declined', 'completed') " +
                "       OR COALESCE(r.Status, '') IN ('cancelled', 'completed')) " +
                "ORDER BY COALESCE(r.Departure_Date, b.Departure_Date) DESC, b.Booking_ID DESC";

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, passengerEmail);
                java.util.List<UpcomingRide> list = new java.util.ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new UpcomingRide(
                            String.valueOf(rs.getInt("Booking_ID")),
                            rs.getString("Booking_Status"),
                            rs.getString("Origin"),
                            rs.getString("Destination"),
                            rs.getString("Departure_Date"),
                            rs.getInt("Seats_Left"),
                            rs.getString("Driver_Name"),
                            rs.getString("Vehicle_Info")
                        ));
                    }
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("getRideHistoryForPassenger failed", e);
        }
    }

    public static boolean cancelUpcomingRideForPassenger(String passengerEmail, String bookingId) {
        String selectSql =
            "SELECT b.Status AS Booking_Status, r.Ride_ID, r.Status AS Ride_Status, r.Departure_Date " +
            "FROM Bookings b " +
            "JOIN Users pu ON pu.User_ID = b.User_ID " +
            "JOIN Rides r ON r.Ride_ID = b.Ride_ID " +
            "WHERE b.Booking_ID = ? " +
            "  AND pu.Email = ? " +
            "  AND pu.Account_Status = 'active' " +
            "FOR UPDATE";
        String cancelSql = "UPDATE Bookings SET Status = 'cancelled' WHERE Booking_ID = ?";
        String increaseSeatSql = "UPDATE Rides SET Seats_Left = Seats_Left + 1 WHERE Ride_ID = ?";

        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            refreshRideStatuses(c);

            String bookingStatus;
            int rideId;
            String rideStatus;
            Timestamp departureDate;

            try (PreparedStatement selectStatement = c.prepareStatement(selectSql)) {
                selectStatement.setInt(1, Integer.parseInt(bookingId));
                selectStatement.setString(2, passengerEmail);
                try (ResultSet rs = selectStatement.executeQuery()) {
                    if (!rs.next()) {
                        c.rollback();
                        return false;
                    }
                    bookingStatus = rs.getString("Booking_Status");
                    rideId = rs.getInt("Ride_ID");
                    rideStatus = rs.getString("Ride_Status");
                    departureDate = rs.getTimestamp("Departure_Date");
                }
            }

            boolean hasDeparted = departureDate == null || !departureDate.after(new Timestamp(System.currentTimeMillis()));
            if (hasDeparted || "completed".equalsIgnoreCase(rideStatus) ||
                (!"pending".equalsIgnoreCase(bookingStatus) && !"accepted".equalsIgnoreCase(bookingStatus))) {
                c.rollback();
                return false;
            }

            try (PreparedStatement cancelStatement = c.prepareStatement(cancelSql);
                 PreparedStatement increaseSeatStatement = c.prepareStatement(increaseSeatSql)) {

                if ("accepted".equalsIgnoreCase(bookingStatus)) {
                    increaseSeatStatement.setInt(1, rideId);
                    increaseSeatStatement.executeUpdate();
                }

                cancelStatement.setInt(1, Integer.parseInt(bookingId));
                boolean cancelled = cancelStatement.executeUpdate() > 0;

                refreshRideStatuses(c);
                c.commit();
                return cancelled;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("cancelUpcomingRideForPassenger failed", e);
        }
    }

    public static java.util.List<PassengerRequest> getPassengerRequestsForDriver(String driverEmail) {
        try (Connection c = DBConnection.get()) {
            refreshRideStatuses(c);

            String sql =
                "SELECT b.Booking_ID, b.Status AS Booking_Status, b.Booking_Timestamp, " +
                "       CONCAT(pu.First_Name, ' ', LEFT(pu.Last_Name, 1), '.') AS Passenger_Name, " +
                "       b.Origin, b.Destination, b.Departure_Date, b.Seats_Requested AS Seats_Left " +
                "FROM Bookings b " +
                "JOIN Users pu ON pu.User_ID = b.User_ID " +
                "WHERE b.Status = 'pending' AND b.Ride_ID IS NULL " +
                "ORDER BY b.Booking_Timestamp DESC";

            java.util.List<PassengerRequest> list = new java.util.ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new PassengerRequest(
                            String.valueOf(rs.getInt("Booking_ID")),
                            rs.getString("Passenger_Name"),
                            rs.getString("Origin"),
                            rs.getString("Destination"),
                            rs.getString("Departure_Date"),
                            rs.getInt("Seats_Left"),
                            rs.getString("Booking_Status"),
                            rs.getString("Booking_Timestamp")
                        ));
                    }
                }
            }

            return list;
        } catch (SQLException e) {
            throw new RuntimeException("getPassengerRequestsForDriver failed", e);
        }
    }

    public static boolean updatePassengerRequestStatus(String driverEmail, String bookingId, String newStatus) {
        if (!"accepted".equalsIgnoreCase(newStatus) && !"declined".equalsIgnoreCase(newStatus)) {
            return false;
        }

        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            refreshRideStatuses(c);

            // Lock the booking request row.
            String bookingSelect =
                "SELECT b.Status AS Booking_Status, b.Ride_ID, b.Origin, b.Destination, b.Departure_Date, b.Seats_Requested " +
                "FROM Bookings b " +
                "WHERE b.Booking_ID = ? FOR UPDATE";

            String bookingStatus;
            Integer existingRideId;
            String origin;
            String destination;
            Timestamp departureDate;
            Integer seatsRequested;

            try (PreparedStatement ps = c.prepareStatement(bookingSelect)) {
                ps.setInt(1, Integer.parseInt(bookingId));
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        c.rollback();
                        return false;
                    }
                    bookingStatus = rs.getString("Booking_Status");
                    existingRideId = rs.getObject("Ride_ID", Integer.class);
                    origin = rs.getString("Origin");
                    destination = rs.getString("Destination");
                    departureDate = rs.getTimestamp("Departure_Date");
                    seatsRequested = rs.getObject("Seats_Requested", Integer.class);
                }
            }

            // Only pending, unassigned requests can be acted on.
            if (!"pending".equalsIgnoreCase(bookingStatus) || existingRideId != null) {
                c.rollback();
                return false;
            }

            if ("declined".equalsIgnoreCase(newStatus)) {
                // Do not mark declined; keep request available for other drivers.
                c.commit();
                return true;
            }

            // accepted: choose driver's first vehicle and create a Ride to fulfill this request.
            int driverId;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT User_ID FROM Users WHERE Email = ? AND Account_Status = 'active' LIMIT 1")) {
                ps.setString(1, driverEmail);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { c.rollback(); return false; }
                    driverId = rs.getInt("User_ID");
                }
            }

            int vehicleId;
            int totalSeats;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT Vehicle_ID, Total_Seats FROM Vehicles WHERE Driver_ID = ? ORDER BY Vehicle_ID ASC LIMIT 1")) {
                ps.setInt(1, driverId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { c.rollback(); return false; }
                    vehicleId = rs.getInt("Vehicle_ID");
                    totalSeats = rs.getInt("Total_Seats");
                }
            }

            if (seatsRequested == null || seatsRequested <= 0 || totalSeats < seatsRequested) {
                c.rollback();
                return false;
            }

            int seatsLeftAfter = totalSeats - seatsRequested;
            String rideStatus = seatsLeftAfter <= 0 ? "full" : "open";

            int rideId;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO Rides (Driver_ID, Vehicle_ID, Origin, Destination, Departure_Date, Seats_Left, Status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, driverId);
                ps.setInt(2, vehicleId);
                ps.setString(3, origin);
                ps.setString(4, destination);
                ps.setTimestamp(5, departureDate);
                ps.setInt(6, seatsLeftAfter);
                ps.setString(7, rideStatus);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) { c.rollback(); return false; }
                    rideId = keys.getInt(1);
                }
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE Bookings SET Ride_ID = ?, Status = 'accepted' WHERE Booking_ID = ?")) {
                ps.setInt(1, rideId);
                ps.setInt(2, Integer.parseInt(bookingId));
                boolean updated = ps.executeUpdate() > 0;
                c.commit();
                return updated;
            }
        } catch (SQLException e) {
            throw new RuntimeException("updatePassengerRequestStatus failed", e);
        }
    }

    public static void createRide(String driverEmail, String vehicleId, String origin, String destination, String departureDate, int seatsLeft) {
        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            String insertRide =
                "INSERT INTO Rides (Driver_ID, Vehicle_ID, Origin, Destination, Departure_Date, Seats_Left, Status) " +
                "SELECT d.User_ID, v.Vehicle_ID, ?, ?, ?, ?, 'open' " +
                "FROM Users u " +
                "JOIN Drivers d ON d.User_ID = u.User_ID " +
                "JOIN Vehicles v ON v.Vehicle_ID = ? AND v.Driver_ID = d.User_ID " +
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

    /** Returns all rides created by this driver, most recent first. */
    public static java.util.List<Ride> getRidesForDriver(String driverEmail) {
        try (Connection c = DBConnection.get()) {
            String sql =
                "SELECT r.Ride_ID, r.Origin, r.Destination, r.Departure_Date, r.Seats_Left, r.Status " +
                "FROM Rides r " +
                "JOIN Users u ON u.User_ID = r.Driver_ID " +
                "WHERE u.Email = ? AND u.Account_Status = 'active' " +
                "ORDER BY r.Departure_Date DESC";

            java.util.List<Ride> list = new ArrayList<>();
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

    /**
     * Cancels a ride and notifies all affected passengers via Notifications + Personalizes.
     * Returns true on success, false if ride can't be cancelled.
     */
    public static boolean cancelRide(String driverEmail, String rideId) {
        String verifySQL =
            "SELECT r.Status, r.Origin, r.Destination " +
            "FROM Rides r " +
            "JOIN Users u ON u.User_ID = r.Driver_ID " +
            "WHERE r.Ride_ID = ? AND u.Email = ? AND u.Account_Status = 'active' FOR UPDATE";

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

        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);

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
            java.util.List<Integer> passengerIds = new ArrayList<>();
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
            try { throw new RuntimeException("cancelRide failed", e); }
            finally { }
        }
    }

    /** Returns all notifications for a user, most recent first. */
    public static java.util.List<String[]> getNotificationsForUser(String email) {
        try (Connection c = DBConnection.get()) {
            String sql =
                "SELECT n.Notification_ID, n.Content, n.Timestamp, n.Read_Status " +
                "FROM Notifications n " +
                "JOIN Users u ON u.User_ID = n.User_ID " +
                "WHERE u.Email = ? AND u.Account_Status = 'active' " +
                "ORDER BY n.Timestamp DESC";

            java.util.List<String[]> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String status = rs.getString("Read_Status");
                        if (status == null || status.isBlank()) status = "unread";
                        list.add(new String[]{
                            rs.getString("Notification_ID"),
                            rs.getString("Content"),
                            rs.getString("Timestamp"),
                            status
                        });
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("getNotificationsForUser failed", e);
        }
    }

    /** Marks a notification as read. */
    public static void markNotificationRead(String notificationId) {
        String sql = "UPDATE Notifications SET Read_Status = 'read' WHERE Notification_ID = ?";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(notificationId));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("markNotificationRead failed", e);
        }
    }
}