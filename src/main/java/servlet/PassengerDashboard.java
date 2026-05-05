package servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.Ride;
import model.User;
import store.AppStore;

/**
 * Passenger dashboard page for authenticated non-driver users.
 *
 * The page is still guarded by the session because passenger-specific actions
 * are only available to signed-in users.
 */
@WebServlet("/dashboard/passenger")
public class PassengerDashboard extends HttpServlet {
    /**
     * Forwards signed-in passengers to their dashboard view.
     *
     * @param req current request used to validate the session
     * @param resp response used to redirect anonymous users
     * @throws ServletException if the JSP dispatch fails
     * @throws IOException if the redirect or forward cannot be written
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Require an active session before exposing dashboard content.
        User user = (User) req.getSession(true).getAttribute("currentUser");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String action = safe(req.getParameter("action"));

        if ("showCreateBookingForm".equals(action)) {
            // Show currently available rides so the passenger can request one by Ride_ID.
            req.setAttribute("availableRides", AppStore.getAvailableRides());
            req.getRequestDispatcher("/WEB-INF/views/create-booking.jsp").forward(req, resp);
            return;
        }

        // Load available rides for display
        String destinationFilter = safe(req.getParameter("destination")).toLowerCase();
String departureFilter = safe(req.getParameter("departureTime"));

List<Ride> rides = AppStore.getAvailableRides();
List<Ride> filteredRides = new ArrayList<>();

for (Ride ride : rides) {
    boolean matches = true;

    // Filter by destination
    if (!destinationFilter.isBlank() &&
        !ride.getDestination().toLowerCase().contains(destinationFilter)) {
        matches = false;
    }

    // Filter by departure time
    if (!departureFilter.isBlank()) {
        String formattedInput = departureFilter.replace("T", " ");
        if (!ride.getDepartureDate().contains(formattedInput)) {
            matches = false;
        }
    }

    if (matches) {
        filteredRides.add(ride);
    }
}

// Send filtered rides instead of all rides
req.setAttribute("availableRides", filteredRides);
        req.setAttribute("notifications", AppStore.getNotificationsForUser(user.getEmail()));
        req.setAttribute("upcomingRides", AppStore.getUpcomingRidesForPassenger(user.getEmail()));

        req.getRequestDispatcher("/WEB-INF/views/passenger-dashboard.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession(true).getAttribute("currentUser");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String action = safe(req.getParameter("action"));

        if ("processCreateBooking".equals(action)) {
            String origin = safe(req.getParameter("origin"));
            String destination = safe(req.getParameter("destination"));
            String departureDate = safe(req.getParameter("departureDate"));
            String seatsNeeded = safe(req.getParameter("seatsLeft"));

            if (!origin.isBlank() && !destination.isBlank() && !departureDate.isBlank() && !seatsNeeded.isBlank()) {
                try {
                    int seats = Integer.parseInt(seatsNeeded);
                    // Standardize HTML datetime-local 'T' separator for MySQL DATETIME.
                    String sqlTimestamp = departureDate.replace("T", " ") + ":00";
                    AppStore.createBooking(user.getEmail(), origin, destination, sqlTimestamp, seats);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if ("processRideRequest".equals(action)) {
            String rideId = safe(req.getParameter("rideId"));
            if (!rideId.isBlank()) {
                try {
                    AppStore.requestSeatOnRide(user.getEmail(), Integer.parseInt(rideId));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if ("cancelUpcomingRide".equals(action)) {
            String bookingId = safe(req.getParameter("bookingId"));
            if (!bookingId.isBlank()) {
                AppStore.cancelUpcomingRideForPassenger(user.getEmail(), bookingId);
            }
        }

        if ("markNotifRead".equals(action)) {
            String notifId = safe(req.getParameter("notifId"));
            if (!notifId.isBlank()) {
                AppStore.markNotificationRead(notifId);
            }
        }

        resp.sendRedirect(req.getContextPath() + "/dashboard/passenger");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
