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

/**
 * Data access layer for passenger bookings and booking-requests.
 *
 * This repository uses explicit transactions and row locks to avoid race
 * conditions such as overbooking seats or multiple drivers accepting the
 * same request.
 */
public final class BookingRepository {
    private BookingRepository() {}

    /**
     * Creates an accepted booking for an existing ride and decrements ride seats atomically.
     *
     * Returns false when the ride does not exist, is not open, has already departed, the
     * passenger account is not active, or there is insufficient remaining capacity.
     *
     * @param passengerEmail authenticated passenger email
     * @param rideId ride identifier
     * @param seatsRequested number of seats to book
     * @return true if booking is created and seats are decremented
     */
    public static boolean bookExistingRide(String passengerEmail, String rideId, int seatsRequested) {
        if (seatsRequested <= 0) return false;

        /*
         * Locks the selected ride row to prevent concurrent overbooking.
         * Used with a transaction that inserts a booking and decrements Seats_Left atomically.
         */
        String lockRideSql =
                "SELECT r.Ride_ID, r.Origin, r.Destination, r.Departure_Date, r.Seats_Left, r.Status " +
                "FROM Rides r WHERE r.Ride_ID = ? FOR UPDATE";

        /*
         * Creates an accepted booking for the active passenger (by email) against a specific ride.
         * Uses INSERT ... SELECT to enforce active account and avoid passing passenger User_ID from the client.
         */
        String insertBookingSql =
                "INSERT INTO Bookings (User_ID, Ride_ID, Origin, Destination, Departure_Date, Seats_Requested, Booking_Timestamp, Status) " +
                "SELECT u.User_ID, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 'accepted' " +
                "FROM Users u WHERE u.Email = ? AND u.Account_Status = 'active' LIMIT 1";

        /* Decrements available seats for the ride once booking is accepted. */
        String decrementSeatsSql = "UPDATE Rides SET Seats_Left = Seats_Left - ? WHERE Ride_ID = ?";

        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            RideStatusStore.refreshRideStatuses(c);

            int ridePk;
            String origin;
            String destination;
            Timestamp departureDate;
            int seatsLeft;
            String rideStatus;

            try (PreparedStatement ps = c.prepareStatement(lockRideSql)) {
                ps.setInt(1, Integer.parseInt(rideId));
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { c.rollback(); return false; }
                    ridePk = rs.getInt("Ride_ID");
                    origin = rs.getString("Origin");
                    destination = rs.getString("Destination");
                    departureDate = rs.getTimestamp("Departure_Date");
                    seatsLeft = rs.getInt("Seats_Left");
                    rideStatus = rs.getString("Status");
                }
            }

            if (departureDate == null || !departureDate.after(new Timestamp(System.currentTimeMillis()))) {
                c.rollback();
                return false;
            }
            if (!"open".equalsIgnoreCase(rideStatus)) {
                c.rollback();
                return false;
            }
            if (seatsLeft < seatsRequested) {
                c.rollback();
                return false;
            }

            try (PreparedStatement psBooking = c.prepareStatement(insertBookingSql);
                 PreparedStatement psRide = c.prepareStatement(decrementSeatsSql)) {
                psBooking.setInt(1, ridePk);
                psBooking.setString(2, origin);
                psBooking.setString(3, destination);
                psBooking.setTimestamp(4, departureDate);
                psBooking.setInt(5, seatsRequested);
                psBooking.setString(6, passengerEmail);

                if (psBooking.executeUpdate() == 0) { c.rollback(); return false; }

                psRide.setInt(1, seatsRequested);
                psRide.setInt(2, ridePk);
                if (psRide.executeUpdate() == 0) { c.rollback(); return false; }

                RideStatusStore.refreshRideStatuses(c);
                LogRepository.log("BOOKING_CREATED", null, null, null, "Passenger " + passengerEmail + " booked ride " + rideId);
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("bookExistingRide failed", e);
        }
    }

    /**
     * Creates a pending booking request that is not yet assigned to a ride.
     *
     * The passenger identity is resolved server-side from the email for an active account.
     *
     * @param passengerEmail authenticated passenger email
     * @param origin requested origin
     * @param destination requested destination
     * @param departureDate requested departure date/time string as provided by the UI
     * @param seatsLeft seats requested (parameter name preserved from UI)
     */
    public static void createBooking(String passengerEmail, String origin, String destination, String departureDate, int seatsLeft) {
        /*
         * Creates a pending passenger request not yet assigned to a ride (Ride_ID is NULL).
         * Passenger identity is resolved server-side from an active account email.
         */
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

    /**
     * Fetches upcoming bookings for a passenger, including both ride-linked bookings and unassigned requests.
     *
     * @param passengerEmail authenticated passenger email
     * @return upcoming rides list ordered by departure ascending
     */
    public static List<UpcomingRide> getUpcomingRidesForPassenger(String passengerEmail) {
        try (Connection c = DBConnection.get()) {
            RideStatusStore.refreshRideStatuses(c);

            /*
             * Lists upcoming bookings for the active passenger (pending/accepted only).
             * Uses COALESCE to support both ride-linked bookings and unassigned booking requests.
             */
            String sql =
                    "SELECT b.Booking_ID, b.Ride_ID, b.Status AS Booking_Status, r.Status AS Ride_Status, " +
                    "       COALESCE(r.Origin, b.Origin) AS Origin, " +
                    "       COALESCE(r.Destination, b.Destination) AS Destination, " +
                    "       COALESCE(r.Departure_Date, b.Departure_Date) AS Departure_Date, " +
                    "       CASE WHEN b.Ride_ID IS NULL THEN b.Seats_Requested ELSE r.Seats_Left END AS Seats_Left, " +
                    "       CONCAT(du.First_Name, ' ', LEFT(du.Last_Name, 1), '.') AS Driver_Name, " +
                    "       CONCAT(v.Color, ' ', v.Make, ' ', v.Model, ' (Plate ', v.License_Plate, ')') AS Vehicle_Info, " +
                    "       du.Gender AS Driver_Gender " +
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
                        Integer ridePk = rs.getObject("Ride_ID", Integer.class);
                        list.add(new UpcomingRide(
                                String.valueOf(rs.getInt("Booking_ID")),
                                ridePk == null ? null : String.valueOf(ridePk),
                                rs.getString("Booking_Status"),
                                rs.getString("Ride_Status"),
                                rs.getString("Origin"),
                                rs.getString("Destination"),
                                rs.getString("Departure_Date"),
                                rs.getInt("Seats_Left"),
                                rs.getString("Driver_Name"),
                                rs.getString("Vehicle_Info"),
                                rs.getString("Driver_Gender")
                        ));
                    }
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("getUpcomingRidesForPassenger failed", e);
        }
    }

    /**
     * Fetches non-upcoming booking history for a passenger.
     *
     * Includes bookings that are cancelled, declined, completed, or whose linked ride is
     * cancelled or completed.
     *
     * @param passengerEmail authenticated passenger email
     * @return history list ordered most recent first
     */
    public static List<UpcomingRide> getRideHistoryForPassenger(String passengerEmail) {
        try (Connection c = DBConnection.get()) {
            RideStatusStore.refreshRideStatuses(c);

            /*
             * Lists non-upcoming bookings for the active passenger:
             * - bookings explicitly cancelled/declined/completed
             * - or bookings whose linked ride was cancelled/completed
             *
             * Orders most recent first for history view.
             */
            String sql =
                    "SELECT b.Booking_ID, b.Ride_ID, b.Status AS Booking_Status, r.Status AS Ride_Status, " +
                    "       COALESCE(r.Origin, b.Origin) AS Origin, " +
                    "       COALESCE(r.Destination, b.Destination) AS Destination, " +
                    "       COALESCE(r.Departure_Date, b.Departure_Date) AS Departure_Date, " +
                    "       CASE WHEN b.Ride_ID IS NULL THEN b.Seats_Requested ELSE r.Seats_Left END AS Seats_Left, " +
                    "       CONCAT(du.First_Name, ' ', LEFT(du.Last_Name, 1), '.') AS Driver_Name, " +
                    "       CONCAT(v.Color, ' ', v.Make, ' ', v.Model, ' (Plate ', v.License_Plate, ')') AS Vehicle_Info, " +
                    "       du.Gender AS Driver_Gender " +
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
                        Integer ridePk = rs.getObject("Ride_ID", Integer.class);
                        list.add(new UpcomingRide(
                                String.valueOf(rs.getInt("Booking_ID")),
                                ridePk == null ? null : String.valueOf(ridePk),
                                rs.getString("Booking_Status"),
                                rs.getString("Ride_Status"),
                                rs.getString("Origin"),
                                rs.getString("Destination"),
                                rs.getString("Departure_Date"),
                                rs.getInt("Seats_Left"),
                                rs.getString("Driver_Name"),
                                rs.getString("Vehicle_Info"),
                                rs.getString("Driver_Gender")
                        ));
                    }
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("getRideHistoryForPassenger failed", e);
        }
    }

    /**
     * Cancels a passenger booking or booking-request.
     *
     * If the booking is ride-linked and accepted, capacity is returned to the ride.
     * Uses row locks inside a transaction to avoid races with other updates.
     *
     * @param passengerEmail authenticated passenger email
     * @param bookingId booking identifier
     * @return true if a row is updated to cancelled
     */
    public static boolean cancelUpcomingRideForPassenger(String passengerEmail, String bookingId) {
        /*
         * Cancels a passenger booking/request and (when applicable) returns seats to the ride.
         * Uses row locking to avoid races with driver actions and status refresh to keep ride state consistent.
         */
        String selectSql =
                "SELECT b.Status AS Booking_Status, b.Ride_ID, b.Seats_Requested, r.Status AS Ride_Status, " +
                "       COALESCE(r.Departure_Date, b.Departure_Date) AS Departure_Date " +
                "FROM Bookings b " +
                "JOIN Users pu ON pu.User_ID = b.User_ID " +
                "LEFT JOIN Rides r ON r.Ride_ID = b.Ride_ID " +
                "WHERE b.Booking_ID = ? " +
                "  AND pu.Email = ? " +
                "  AND pu.Account_Status = 'active' " +
                "FOR UPDATE";
        /* Marks the booking cancelled (works for both ride-linked and unassigned requests). */
        String cancelSql = "UPDATE Bookings SET Status = 'cancelled' WHERE Booking_ID = ?";
        /* Restores ride capacity when an accepted booking is cancelled. */
        String increaseSeatSql = "UPDATE Rides SET Seats_Left = Seats_Left + ? WHERE Ride_ID = ?";

        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            RideStatusStore.refreshRideStatuses(c);

            String bookingStatus;
            Integer rideId;
            Integer seatsRequested;
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
                    rideId = rs.getObject("Ride_ID", Integer.class);
                    seatsRequested = rs.getObject("Seats_Requested", Integer.class);
                    rideStatus = rs.getString("Ride_Status");
                    departureDate = rs.getTimestamp("Departure_Date");
                }
            }

            // Pending requests without an assigned ride can be cancelled any time before their requested departure.
            if (rideId == null) {
                boolean hasDeparted = departureDate == null || !departureDate.after(new Timestamp(System.currentTimeMillis()));
                if (hasDeparted || !"pending".equalsIgnoreCase(bookingStatus)) {
                    c.rollback();
                    return false;
                }

                try (PreparedStatement cancelStatement = c.prepareStatement(cancelSql)) {
                    cancelStatement.setInt(1, Integer.parseInt(bookingId));
                    boolean cancelled = cancelStatement.executeUpdate() > 0;
                    c.commit();
                    return cancelled;
                } catch (SQLException e) {
                    c.rollback();
                    throw e;
                } finally {
                    c.setAutoCommit(true);
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
                    int delta = (seatsRequested == null || seatsRequested <= 0) ? 1 : seatsRequested;
                    increaseSeatStatement.setInt(1, delta);
                    increaseSeatStatement.setInt(2, rideId);
                    increaseSeatStatement.executeUpdate();
                }

                cancelStatement.setInt(1, Integer.parseInt(bookingId));
                boolean cancelled = cancelStatement.executeUpdate() > 0;

                RideStatusStore.refreshRideStatuses(c);
                LogRepository.log("BOOKING_CANCELLED", null, null, Integer.parseInt(bookingId), "Passenger " + passengerEmail + " cancelled booking " + bookingId);
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

    /**
     * Lists unassigned passenger requests that drivers can fulfill by creating a matching ride.
     *
     * @param driverEmail authenticated driver email (currently not used for filtering)
     * @return list of pending, unassigned requests
     */
    public static List<PassengerRequest> getPassengerRequestsForDriver(String driverEmail) {
        try (Connection c = DBConnection.get()) {
            RideStatusStore.refreshRideStatuses(c);

            /*
             * Lists unassigned passenger requests awaiting a driver's offer (Ride_ID is NULL, Status pending).
             * Shown on the driver dashboard; ordered newest-first for triage.
             */
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

    /**
     * Accepts a pending, unassigned passenger request by creating a ride under the driver and linking the booking.
     *
     * Only "accepted" is supported; other statuses return false.
     *
     * @param driverEmail authenticated driver email
     * @param bookingId booking identifier for the request
     * @param newStatus requested status (must be accepted)
     * @param vehicleId vehicle to use when accepting; required for accept
     * @return true if the booking row is updated
     */
    public static boolean updatePassengerRequestStatus(String driverEmail, String bookingId, String newStatus, String vehicleId) {
        if (!"accepted".equalsIgnoreCase(newStatus)) {
            return false;
        }
        if (vehicleId == null || vehicleId.isBlank()) {
            return false;
        }

        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            RideStatusStore.refreshRideStatuses(c);

            // Lock the booking request row.
            /* Locks the booking request to prevent multiple drivers acting on it. */
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

            // accepted: choose driver's first vehicle and create a Ride to fulfill this request.
            int driverId;
            try (PreparedStatement ps = c.prepareStatement(
                    /* Resolves the acting driver from the authenticated email (active accounts only). */
                    "SELECT User_ID FROM Users WHERE Email = ? AND Account_Status = 'active' LIMIT 1")) {
                ps.setString(1, driverEmail);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { c.rollback(); return false; }
                    driverId = rs.getInt("User_ID");
                }
            }

            int vehicleIdInt;
            int totalSeats;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT v.Vehicle_ID, v.Total_Seats FROM Vehicles v " +
                    "JOIN Users u ON u.User_ID = v.Driver_ID " +
                    "WHERE v.Vehicle_ID = ? AND v.Driver_ID = (SELECT User_ID FROM Users WHERE Email = ? AND Account_Status = 'active' LIMIT 1) " +
                    "AND v.Vehicle_Status = 'active' LIMIT 1")) {
                ps.setInt(1, Integer.parseInt(vehicleId));
                ps.setString(2, driverEmail);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { c.rollback(); return false; }
                    vehicleIdInt = rs.getInt("Vehicle_ID");
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
                    /*
                     * Creates a ride offering that satisfies the request and leaves remaining capacity (Seats_Left).
                     * The resulting ride status is derived from remaining seats (open/full).
                     */
                    "INSERT INTO Rides (Driver_ID, Vehicle_ID, Origin, Destination, Departure_Date, Seats_Left, Status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, driverId);
                ps.setInt(2, vehicleIdInt);
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
                    /* Links the booking to the newly created ride and marks it accepted in one update. */
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

