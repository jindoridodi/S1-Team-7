package model;

/**
 * Immutable driver-facing booking request row shown on the driver dashboard.
 */
public class PassengerRequest {
    private final String bookingId;
    private final String passengerName;
    private final String origin;
    private final String destination;
    private final String departureDate;
    private final int requestedSeats;
    private final String bookingStatus;
    private final String bookingTimestamp;

    /**
     * Creates a driver-facing passenger request row.
     *
     * @param bookingId booking identifier
     * @param passengerName formatted passenger name
     * @param origin requested origin
     * @param destination requested destination
     * @param departureDate requested departure date/time
     * @param requestedSeats seats requested
     * @param bookingStatus current request status
     * @param bookingTimestamp request creation time
     */
    public PassengerRequest(
        String bookingId,
        String passengerName,
        String origin,
        String destination,
        String departureDate,
        int requestedSeats,
        String bookingStatus,
        String bookingTimestamp
    ) {
        this.bookingId = bookingId;
        this.passengerName = passengerName;
        this.origin = origin;
        this.destination = destination;
        this.departureDate = departureDate;
        this.requestedSeats = requestedSeats;
        this.bookingStatus = bookingStatus;
        this.bookingTimestamp = bookingTimestamp;
    }

    public String getBookingId() { return bookingId; }
    public String getPassengerName() { return passengerName; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public String getDepartureDate() { return departureDate; }
    public int getRequestedSeats() { return requestedSeats; }
    public String getBookingStatus() { return bookingStatus; }
    public String getBookingTimestamp() { return bookingTimestamp; }
}