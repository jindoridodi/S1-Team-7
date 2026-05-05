package repository;

import db.DBConnection;
import model.PassengerRequest;
import model.UpcomingRide;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public final class BookingRepository {
    private BookingRepository() {}

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

    public static List<UpcomingRide> getUpcomingRidesForPassenger(String passengerEmail) {
        try (Connection c = DBConnection.get()) {
            RideStatusStore.refreshRideStatuses(c);

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
                List<UpcomingRide> list = new ArrayList<>();
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

    public static List<UpcomingRide> getRideHistoryForPassenger(String passengerEmail) {
        try (Connection c = DBConnection.get()) {
            RideStatusStore.refreshRideStatuses(c);

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
                List<UpcomingRide> list = new ArrayList<>();
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
            RideStatusStore.refreshRideStatuses(c);

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

                RideStatusStore.refreshRideStatuses(c);
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

    public static List<PassengerRequest> getPassengerRequestsForDriver(String driverEmail) {
        try (Connection c = DBConnection.get()) {
            RideStatusStore.refreshRideStatuses(c);

            String sql =
                    "SELECT b.Booking_ID, b.Status AS Booking_Status, b.Booking_Timestamp, " +
                    "       CONCAT(pu.First_Name, ' ', LEFT(pu.Last_Name, 1), '.') AS Passenger_Name, " +
                    "       b.Origin, b.Destination, b.Departure_Date, b.Seats_Requested AS Seats_Left " +
                    "FROM Bookings b " +
                    "JOIN Users pu ON pu.User_ID = b.User_ID " +
                    "WHERE b.Status = 'pending' AND b.Ride_ID IS NULL " +
                    "ORDER BY b.Booking_Timestamp DESC";

            List<PassengerRequest> list = new ArrayList<>();
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
            RideStatusStore.refreshRideStatuses(c);

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
}

