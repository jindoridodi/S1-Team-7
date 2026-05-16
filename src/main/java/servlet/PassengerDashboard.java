package servlet;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
import repository.ReviewRepository;
import repository.SavedRouteRepository;
import repository.UserRepository;

/**
 * Passenger dashboard page for authenticated non-driver users.
 *
 * Search and booking are separate: search only filters listings; seat requests
 * are submitted explicitly from the confirm-seat flow.
 */
@WebServlet("/dashboard/passenger")
public class PassengerDashboard extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession(true).getAttribute("currentUser");
        if (user == null) {
            String redirect = buildLoginRedirect(req);
            resp.sendRedirect(req.getContextPath() + "/login?redirect=" + redirect);
            return;
        }

        req.setAttribute("driverVerified", user.hasRole("driver") && UserRepository.isVerifiedDriver(user.getEmail()));

        String action = safe(req.getParameter("action"));

        if ("searchRides".equals(action) || hasSearchParams(req)) {
            forwardSearchRides(req, resp, user);
            return;
        }

        if ("showRequestRideForm".equals(action) || "showCreateBookingForm".equals(action)) {
            req.getRequestDispatcher("/WEB-INF/views/create-booking.jsp").forward(req, resp);
            return;
        }

        if ("confirmSeatRequest".equals(action)) {
            String rideId = safe(req.getParameter("rideId"));
            Ride ride = rideId.isBlank() ? null : RideRepository.getRideByIdForSeatRequest(rideId);
            if (ride == null) {
                resp.sendRedirect(req.getContextPath() + "/dashboard/passenger?action=searchRides&error=rideUnavailable");
                return;
            }
            req.setAttribute("ride", ride);
            req.getRequestDispatcher("/WEB-INF/views/confirm-seat-request.jsp").forward(req, resp);
            return;
        }

        if ("showRideHistory".equals(action)) {
            var history = BookingRepository.getRideHistoryForPassenger(user.getEmail());
            req.setAttribute("rideHistory", history);
            attachRideReviews(req, user.getEmail(), history);
            if ("reviewSaved".equals(safe(req.getParameter("msg")))) {
                req.setAttribute("successMessage", "Your note was posted.");
            } else if ("reviewFailed".equals(safe(req.getParameter("msg")))) {
                req.setAttribute("error", "Could not post that note. You can only review completed rides you joined, once per ride.");
            }
            req.getRequestDispatcher("/WEB-INF/views/passenger-ride-history.jsp").forward(req, resp);
            return;
        }

        if ("showSavedRoutes".equals(action)) {
            req.setAttribute("savedRoutes", SavedRouteRepository.getRoutesForUser(user.getEmail()));
            req.getRequestDispatcher("/WEB-INF/views/passenger-saved-routes.jsp").forward(req, resp);
            return;
        }

        req.setAttribute("notifications", NotificationRepository.getNotificationsForUser(user.getEmail()));
        req.setAttribute("upcomingRides", BookingRepository.getUpcomingRidesForPassenger(user.getEmail()));

        String msg = safe(req.getParameter("msg"));
        if ("seatRequested".equals(msg)) {
            req.setAttribute("successMessage", "Your seat request was submitted. See Upcoming Rides below.");
        } else if ("openRequestPosted".equals(msg)) {
            req.setAttribute("successMessage", "Your open ride request was posted for drivers.");
        }

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

        if ("processCreateBooking".equals(action) || "postOpenRideRequest".equals(action)) {
            String origin = safe(req.getParameter("origin"));
            String destination = safe(req.getParameter("destination"));
            String departureDate = safe(req.getParameter("departureDate"));
            String seatsNeeded = safe(req.getParameter("seatsLeft"));

            if (!origin.isBlank() && !destination.isBlank() && !departureDate.isBlank() && !seatsNeeded.isBlank()) {
                try {
                    int seats = Integer.parseInt(seatsNeeded);
                    String sqlTimestamp = departureDate.replace("T", " ") + ":00";
                    BookingRepository.createBooking(user.getEmail(), origin, destination, sqlTimestamp, seats);
                    resp.sendRedirect(req.getContextPath() + "/dashboard/passenger?msg=openRequestPosted");
                    return;
                } catch (NumberFormatException ignored) {
                }
            }
            resp.sendRedirect(req.getContextPath() + "/dashboard/passenger?action=showRequestRideForm&error=requestInvalid");
            return;
        }

        if ("requestSeatOnRide".equals(action) || "bookExistingRide".equals(action)) {
            String rideId = safe(req.getParameter("rideId"));
            String seatsRequested = safe(req.getParameter("seatsRequested"));
            if (!rideId.isBlank() && !seatsRequested.isBlank()) {
                try {
                    int seats = Integer.parseInt(seatsRequested);
                    BookingRepository.requestSeatOnRide(user.getEmail(), rideId, seats);
                } catch (NumberFormatException ignored) {
                }
            }
            resp.sendRedirect(req.getContextPath() + "/dashboard/passenger?msg=seatRequested");
            return;
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
            resp.sendRedirect(req.getContextPath() + "/dashboard/passenger?action=showSavedRoutes");
            return;
        }

        if ("deleteRoute".equals(action)) {
            String routeId = safe(req.getParameter("routeId"));
            if (!routeId.isBlank()) {
                SavedRouteRepository.deleteRoute(user.getEmail(), routeId);
            }
            resp.sendRedirect(req.getContextPath() + "/dashboard/passenger?action=showSavedRoutes");
            return;
        }

        if ("submitReview".equals(action)) {
            String rideId = safe(req.getParameter("rideId"));
            String comments = safe(req.getParameter("comments"));
            Integer rating = parseOptionalRating(req.getParameter("ratingStars"));
            boolean ok = ReviewRepository.submitReview(user.getEmail(), rideId, rating, comments);
            resp.sendRedirect(req.getContextPath() + "/dashboard/passenger?action=showRideHistory&msg="
                + (ok ? "reviewSaved" : "reviewFailed"));
            return;
        }

        resp.sendRedirect(req.getContextPath() + "/dashboard/passenger");
    }

    private void forwardSearchRides(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        String originFilter = safe(req.getParameter("searchOrigin"));
        String destinationFilter = safe(req.getParameter("searchDestination"));
        String dateFilter = safe(req.getParameter("searchDate"));

        boolean searchPerformed = !originFilter.isBlank() || !destinationFilter.isBlank() || !dateFilter.isBlank();

        List<Ride> filteredRides = filterRides(
                RideRepository.getAvailableRides(),
                originFilter,
                destinationFilter,
                dateFilter);

        req.setAttribute("searchOrigin", originFilter);
        req.setAttribute("searchDestination", destinationFilter);
        req.setAttribute("searchDate", dateFilter);
        req.setAttribute("searchPerformed", searchPerformed);
        req.setAttribute("availableRides", filteredRides);
        req.setAttribute("savedRoutes", SavedRouteRepository.getRoutesForUser(user.getEmail()));
        req.setAttribute("msg", safe(req.getParameter("msg")));

        if ("rideUnavailable".equals(safe(req.getParameter("error")))) {
            req.setAttribute("error", "That ride is no longer available.");
        }

        req.getRequestDispatcher("/WEB-INF/views/search-rides.jsp").forward(req, resp);
    }

    private static List<Ride> filterRides(List<Ride> rides, String originFilter, String destinationFilter, String dateFilter) {
        List<Ride> filtered = new ArrayList<>();

        for (Ride ride : rides) {
            boolean matches = true;

            if (!originFilter.isBlank()) {
                boolean ok = locationMatches(ride.getOrigin(), originFilter);
                if (!ok && destinationFilter.isBlank()) {
                    ok = locationMatches(ride.getDestination(), originFilter);
                }
                if (!ok) {
                    matches = false;
                }
            }

            if (matches && !destinationFilter.isBlank()) {
                boolean ok = locationMatches(ride.getDestination(), destinationFilter);
                if (!ok && originFilter.isBlank()) {
                    ok = locationMatches(ride.getOrigin(), destinationFilter);
                }
                if (!ok) {
                    matches = false;
                }
            }

            if (matches && !dateFilter.isBlank()) {
                if (!departureMatchesDateFilter(ride.getDepartureDate(), dateFilter)) {
                    matches = false;
                }
            }

            if (matches) {
                filtered.add(ride);
            }
        }
        return filtered;
    }

    /**
     * Lenient location match: punctuation-insensitive, extra spaces collapsed, and every word in the query
     * must appear as a substring (so word order can differ). If only one of origin/destination is filled in,
     * the query may match either ride endpoint so swapped fields still work.
     */
    private static boolean locationMatches(String rideLocation, String filterRaw) {
        if (filterRaw == null || filterRaw.isBlank()) {
            return true;
        }
        if (rideLocation == null || rideLocation.isBlank()) {
            return false;
        }
        String normRide = normalizeForSearch(rideLocation);
        String normFilter = normalizeForSearch(filterRaw);
        if (normFilter.isEmpty()) {
            return true;
        }
        if (normRide.contains(normFilter)) {
            return true;
        }
        for (String token : normFilter.split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            if (!normRide.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeForSearch(String s) {
        return s.toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static boolean departureMatchesDateFilter(String departureSql, String dateFilter) {
        if (dateFilter == null || dateFilter.isBlank()) {
            return true;
        }
        LocalDate filterDate;
        try {
            filterDate = LocalDate.parse(dateFilter.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            return false;
        }
        LocalDate rideDate = departureToLocalDate(departureSql);
        return rideDate != null && rideDate.equals(filterDate);
    }

    /** Parses typical MySQL datetime / date strings to the calendar day of departure. */
    private static LocalDate departureToLocalDate(String departureSql) {
        if (departureSql == null || departureSql.isBlank()) {
            return null;
        }
        String t = departureSql.trim();
        if (t.length() >= 10 && t.charAt(4) == '-' && t.charAt(7) == '-') {
            try {
                return LocalDate.parse(t.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ignored) {
                // fall through
            }
        }
        DateTimeFormatter[] patterns = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
        };
        for (DateTimeFormatter pattern : patterns) {
            try {
                return LocalDateTime.parse(t, pattern).toLocalDate();
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }

    private static boolean hasSearchParams(HttpServletRequest req) {
        return !safe(req.getParameter("searchOrigin")).isBlank()
            || !safe(req.getParameter("searchDestination")).isBlank()
            || !safe(req.getParameter("searchDate")).isBlank();
    }

    private static String buildLoginRedirect(HttpServletRequest req) {
        String query = req.getQueryString();
        String path = req.getRequestURI();
        if (query != null && !query.isBlank()) {
            path = path + "?" + query;
        }
        return java.net.URLEncoder.encode(path, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void attachRideReviews(
            HttpServletRequest req,
            String userEmail,
            java.util.List<model.UpcomingRide> history) {
        Set<String> rideIds = new HashSet<>();
        for (model.UpcomingRide row : history) {
            if (row.isReviewable()) {
                rideIds.add(row.getRideId());
            }
        }
        if (rideIds.isEmpty()) {
            return;
        }
        req.setAttribute("reviewsByRideId", ReviewRepository.getReviewsByRideIds(rideIds));
        req.setAttribute("myReviewsByRideId", ReviewRepository.getMyReviewsByRideIds(userEmail, rideIds));
    }

    private static Integer parseOptionalRating(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
