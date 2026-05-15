package model;

/**
 * Immutable passenger booking view model for upcoming rides.
 */
public class UpcomingRide {
    private final String bookingId;
    private final String rideId;
    private final String bookingStatus;
    private final String rideStatus;
    private final String origin;
    private final String destination;
    private final String departureDate;
    private final int seats;
    private final String driverName;
    private final String vehicleInfo;
    private final String driverGender;

    /**
     * Creates an upcoming ride row for passenger dashboards.
     */
    public UpcomingRide(
        String bookingId,
        String rideId,
        String bookingStatus,
        String rideStatus,
        String origin,
        String destination,
        String departureDate,
        int seats,
        String driverName,
        String vehicleInfo,
        String driverGender
    ) {
        this.bookingId = bookingId;
        this.rideId = rideId;
        this.bookingStatus = bookingStatus;
        this.rideStatus = rideStatus;
        this.origin = origin;
        this.destination = destination;
        this.departureDate = departureDate;
        this.seats = seats;
        this.driverName = driverName;
        this.vehicleInfo = vehicleInfo;
        this.driverGender = driverGender;
    }

    public String getBookingId() { return bookingId; }
    public String getRideId() { return rideId; }
    public String getBookingStatus() { return bookingStatus; }
    public String getRideStatus() { return rideStatus; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public String getDepartureDate() { return departureDate; }
    public int getSeats() { return seats; }
    public String getDriverName() { return driverName; }
    public String getVehicleInfo() { return vehicleInfo; }
    public String getDriverGender() { return driverGender; }

    /** True when the linked ride finished and the passenger may leave a review. */
    public boolean isReviewable() {
        return rideId != null && !rideId.isBlank()
            && "completed".equalsIgnoreCase(rideStatus);
    }
}
