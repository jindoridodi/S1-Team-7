package model;

/**
 * Immutable ride record used by views and store retrievals.
 */
public class Ride {
    private final String id;
    private final String origin;
    private final String destination;
    private final String departureDate;
    private final int seatsLeft;
    private final String status;
    private final String driverName;
    private final String vehicleInfo;
    /** Driver gender for passenger-facing ride details; null when not applicable. */
    private final String driverGender;

    /**
     * Creates a ride view model.
     *
     * @param id ride identifier
     * @param origin ride origin
     * @param destination ride destination
     * @param departureDate departure date/time string
     * @param seatsLeft remaining seats for booking
     * @param status ride status string
     * @param driverName formatted driver display name (may be null for driver-owned lists)
     * @param vehicleInfo formatted vehicle display string (may be null for driver-owned lists)
     * @param driverGender driver gender from profile for passenger listings; may be null
     */
    public Ride(String id, String origin, String destination, String departureDate, int seatsLeft, String status, String driverName, String vehicleInfo, String driverGender) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        this.departureDate = departureDate;
        this.seatsLeft = seatsLeft;
        this.status = status;
        this.driverName = driverName;
        this.vehicleInfo = vehicleInfo;
        this.driverGender = driverGender;
    }

    public String getId() { return id; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public String getDepartureDate() { return departureDate; }
    public int getSeatsLeft() { return seatsLeft; }
    public String getStatus() { return status; }
    public String getDriverName() { return driverName; }
    public String getVehicleInfo() { return vehicleInfo; }
    public String getDriverGender() { return driverGender; }
}
