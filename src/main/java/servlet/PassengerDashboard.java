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
import repository.BookingRepository;
import repository.NotificationRepository;
import repository.RideRepository;
import repository.SavedRouteRepository;

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
            req.setAttribute("availableRides", RideRepository.getAvailableRides());
            req.getRequestDispatcher("/WEB-INF/views/create-booking.jsp").forward(req, resp);
            return;
        }

        if ("showRideHistory".equals(action)) {
            req.setAttribute("rideHistory", BookingRepository.getRideHistoryForPassenger(user.getEmail()));
            req.getRequestDispatcher("/WEB-INF/views/passenger-ride-history.jsp").forward(req, resp);
            return;
        }

        // Load available rides for display
        String originFilter = safe(req.getParameter("searchOrigin")).toLowerCase();
        String destinationFilter = safe(req.getParameter("searchDestination")).toLowerCase();
        // `searchDate` comes from home.jsp as yyyy-MM-dd.
        String dateFilter = safe(req.getParameter("searchDate"));

        List<Ride> rides = RideRepository.getAvailableRides();
        List<Ride> filteredRides = new ArrayList<>();

        for (Ride ride : rides) {
            boolean matches = true;

            if (!originFilter.isBlank() &&
                (ride.getOrigin() == null || !ride.getOrigin().toLowerCase().contains(originFilter))) {
                matches = false;
            }

            if (!destinationFilter.isBlank() &&
                (ride.getDestination() == null || !ride.getDestination().toLowerCase().contains(destinationFilter))) {
                matches = false;
            }

            if (matches && !dateFilter.isBlank()) {
                // Ride departureDate is typically "yyyy-MM-dd HH:mm:ss" from MySQL.
                String dep = ride.getDepartureDate() == null ? "" : ride.getDepartureDate();
                if (!dep.startsWith(dateFilter)) matches = false;
            }

            if (matches) filteredRides.add(ride);
        }

        req.setAttribute("availableRides", filteredRides);
        req.setAttribute("notifications", NotificationRepository.getNotificationsForUser(user.getEmail()));
        req.setAttribute("savedRoutes", SavedRouteRepository.getRoutesForUser(user.getEmail()));
        req.setAttribute("upcomingRides", BookingRepository.getUpcomingRidesForPassenger(user.getEmail()));

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
                    BookingRepository.createBooking(user.getEmail(), origin, destination, sqlTimestamp, seats);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if ("bookExistingRide".equals(action)) {
            String rideId = safe(req.getParameter("rideId"));
            String seatsRequested = safe(req.getParameter("seatsRequested"));
            if (!rideId.isBlank() && !seatsRequested.isBlank()) {
                try {
                    int seats = Integer.parseInt(seatsRequested);
                    BookingRepository.bookExistingRide(user.getEmail(), rideId, seats);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if ("cancelUpcomingRide".equals(action)) {
            String bookingId = safe(req.getParameter("bookingId"));
            if (!bookingId.isBlank()) {
                BookingRepository.cancelUpcomingRideForPassenger(user.getEmail(), bookingId);
            }
        }

        if ("markNotifRead".equals(action)) {
            String notifId = safe(req.getParameter("notifId"));
            if (!notifId.isBlank()) {
                NotificationRepository.markNotificationRead(user.getEmail(), notifId);
            }
        }

        if ("saveRoute".equals(action)) {
            String start = safe(req.getParameter("startLocation"));
            String end   = safe(req.getParameter("endLocation"));
            if (!start.isBlank() && !end.isBlank()) {
                SavedRouteRepository.saveRoute(user.getEmail(), start, end);
            }
        }
        
        if ("deleteRoute".equals(action)) {
            String routeId = safe(req.getParameter("routeId"));
            if (!routeId.isBlank()) {
                SavedRouteRepository.deleteRoute(user.getEmail(), routeId);
            }
        }

        resp.sendRedirect(req.getContextPath() + "/dashboard/passenger");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
