package servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.User;
import repository.BookingRepository;
import repository.ReviewRepository;
import repository.RideRepository;
import repository.UserRepository;
import repository.VehicleRepository;

import java.util.HashSet;
import java.util.Set;

/**
 * Driver dashboard for viewing and managing the current user's vehicles.
 *
 * The servlet keeps the vehicle list server-rendered so ownership checks and
 * edits are enforced on the backend before the JSP is shown.
 */
@WebServlet("/dashboard/driver")
public class DriverDashboard extends HttpServlet {
    /**
     * Loads the driver's vehicles only after confirming the session is active.
     *
     * @param req current request used to read the session and expose vehicle data
     * @param resp response used to redirect anonymous users
     * @throws ServletException if the JSP dispatch fails
     * @throws IOException if the redirect or forward cannot be written
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Guard the route before any vehicle query runs.
        User user = (User) req.getSession(true).getAttribute("currentUser");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        String status = UserRepository.getDriverVerificationStatus(user.getEmail());

        if (status == null || !status.equalsIgnoreCase("verified")) {
            // Keep JSP rendering in sync with access control:
            // unverified drivers should see the pending verification screen.
            req.setAttribute("pendingVerification", true);
            req.getRequestDispatcher("/WEB-INF/views/driver-dashboard.jsp").forward(req, resp);
            return;
        }
        

        String error = safe(req.getParameter("error"));
        if ("requestNotProcessed".equals(error)) {
            req.setAttribute("error",
                "Could not accept that request. Pick a vehicle with enough seats, or it may have been accepted already.");
        }

        String action = safe(req.getParameter("action"));

        // Navigate to ride creation form if requested
        if ("showCreateRideForm".equals(action)) {
            req.setAttribute("vehicles", VehicleRepository.getVehiclesForOwner(user.getEmail()));
            req.getRequestDispatcher("/WEB-INF/views/create-ride.jsp").forward(req, resp);
            return;
        }

        if ("showVehicles".equals(action)) {
            if ("vehicleInUse".equals(error)) {
                req.setAttribute("error",
                        "Cannot delete this vehicle because it is assigned to an existing ride. Cancel/delete the ride first.");
            }
            req.setAttribute("vehicles", VehicleRepository.getVehiclesForOwner(user.getEmail()));
            req.getRequestDispatcher("/WEB-INF/views/driver-vehicles.jsp").forward(req, resp);
            return;
        }

        if ("showRideHistory".equals(action)) {
            var history = RideRepository.getRideHistoryForDriver(user.getEmail());
            req.setAttribute("driverRideHistory", history);
            attachRideReviews(req, user.getEmail(), history);
            if ("reviewSaved".equals(safe(req.getParameter("msg")))) {
                req.setAttribute("successMessage", "Your note was posted.");
            } else if ("reviewFailed".equals(safe(req.getParameter("msg")))) {
                req.setAttribute("error", "Could not post that note. You can only review completed rides you hosted, once per ride.");
            }
            req.getRequestDispatcher("/WEB-INF/views/driver-ride-history.jsp").forward(req, resp);
            return;
        }

        req.setAttribute("driverVehicles", VehicleRepository.getVehiclesForOwner(user.getEmail()));
        req.setAttribute("passengerRequests", BookingRepository.getPassengerRequestsForDriver(user.getEmail()));
        req.setAttribute("driverRides", RideRepository.getActiveRidesForDriver(user.getEmail()));
        req.getRequestDispatcher("/WEB-INF/views/driver-dashboard.jsp").forward(req, resp);
    }

    /**
     * Handles the dashboard form actions that mutate vehicle data.
     *
     * The page uses one endpoint for multiple buttons, so the action parameter
     * decides which database operation to run.
     *
     * @param req current form submission containing the action and fields
     * @param resp response used to redirect anonymous users and refresh the view
     * @throws IOException if the redirect cannot be written
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession(true).getAttribute("currentUser");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // Enforce the same verification guard on POST actions.
        String status = UserRepository.getDriverVerificationStatus(user.getEmail());
        if (status == null || !status.equalsIgnoreCase("verified")) {
            resp.sendRedirect(req.getContextPath() + "/dashboard/driver?error=pendingVerification");
            return;
        }

        // One endpoint keeps the form simple while still supporting multiple actions.
        String action = req.getParameter("action");

        // Handles the persistence of a new ride post
        if ("processCreateRide".equals(action)) {
            String vehicleId = safe(req.getParameter("vehicleId"));
            String origin = safe(req.getParameter("origin"));
            String destination = safe(req.getParameter("destination"));
            String departureDate = safe(req.getParameter("departureDate")); 
            String seatsLeft = safe(req.getParameter("seatsLeft"));

            if (!vehicleId.isBlank() && !origin.isBlank() && !destination.isBlank() && !departureDate.isBlank()) {
                try {
                    int seats = Integer.parseInt(seatsLeft);
                    // Standardize HTML T-separator for MySQL DATETIME
                    String sqlTimestamp = departureDate.replace("T", " ") + ":00";
                    RideRepository.createRide(user.getEmail(), vehicleId, origin, destination, sqlTimestamp, seats);
                } catch (NumberFormatException ignored) {}
            }
        }

        if ("addVehicle".equals(action)) {
            String make = safe(req.getParameter("make"));
            String model = safe(req.getParameter("model"));
            String color = safe(req.getParameter("color"));
            String plate = safe(req.getParameter("plate"));
            String totalSeats = safe(req.getParameter("totalSeats"));
            // Include insurance field from your database
            String insuranceNum = safe(req.getParameter("insuranceNum")); 

            if (!make.isBlank() && !model.isBlank() && !color.isBlank() && !plate.isBlank() && !totalSeats.isBlank()) {
                try {
                    int seats = Integer.parseInt(totalSeats);
                    if (seats > 0) {
                        VehicleRepository.addVehicle(user.getEmail(), make, model, color, plate, seats, insuranceNum);
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore invalid seat counts and fall through to the redirect.
                }
            }
        }

        if ("deleteVehicle".equals(action)) {
            String vehicleId = safe(req.getParameter("vehicleId"));
            if (!vehicleId.isBlank()) {
                boolean deleted = VehicleRepository.deleteVehicle(user.getEmail(), vehicleId);
                if (!deleted) { /* ignore */ }
            }
        }

        if ("updateVehicle".equals(action)) {
            String vehicleId = safe(req.getParameter("vehicleId"));
            String make = safe(req.getParameter("make"));
            String model = safe(req.getParameter("model"));
            String color = safe(req.getParameter("color"));
            String plate = safe(req.getParameter("plate"));
            String totalSeats = safe(req.getParameter("totalSeats"));
            String insuranceNum = safe(req.getParameter("insuranceNum"));

            if (!vehicleId.isBlank() && !make.isBlank() && !model.isBlank() && !color.isBlank() && !plate.isBlank() && !totalSeats.isBlank()) {
                try {
                    int seats = Integer.parseInt(totalSeats);
                    if (seats > 0) {
                        VehicleRepository.updateVehicle(user.getEmail(), vehicleId, make, model, color, plate, seats, insuranceNum);
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore invalid seat counts and fall through to the redirect.
                }
            }
        }

        if ("processPassengerRequest".equals(action)) {
            String bookingId = safe(req.getParameter("bookingId"));
            String decision = safe(req.getParameter("decision"));
            String vehicleId = safe(req.getParameter("vehicleId"));

            if (!bookingId.isBlank()) {
                String nextStatus = "";
                if ("accept".equalsIgnoreCase(decision)) {
                    nextStatus = "accepted";
                }

                if (!nextStatus.isBlank()) {
                    boolean ok = BookingRepository.updatePassengerRequestStatus(user.getEmail(), bookingId, nextStatus, vehicleId);
                    if (!ok) {
                        resp.sendRedirect(req.getContextPath() + "/dashboard/driver?error=requestNotProcessed");
                        return;
                    }
                }
            }
        }

        if ("addVehicle".equals(action) || "updateVehicle".equals(action) || "deleteVehicle".equals(action)) {
            resp.sendRedirect(req.getContextPath() + "/dashboard/driver?action=showVehicles");
            return;
        }

        if ("cancelRide".equals(action)) {
            String rideId = safe(req.getParameter("rideId"));
            if (!rideId.isBlank()) {
                RideRepository.cancelRide(user.getEmail(), rideId);
            }
        }

        if ("submitReview".equals(action)) {
            String rideId = safe(req.getParameter("rideId"));
            String comments = safe(req.getParameter("comments"));
            Integer rating = parseOptionalRating(req.getParameter("ratingStars"));
            boolean ok = ReviewRepository.submitReview(user.getEmail(), rideId, rating, comments);
            resp.sendRedirect(req.getContextPath() + "/dashboard/driver?action=showRideHistory&msg="
                + (ok ? "reviewSaved" : "reviewFailed"));
            return;
        }

        resp.sendRedirect(req.getContextPath() + "/dashboard/driver");
    }

    private static void attachRideReviews(
            HttpServletRequest req,
            String userEmail,
            java.util.List<model.Ride> history) {
        Set<String> rideIds = new HashSet<>();
        for (model.Ride row : history) {
            if (row.getId() != null && "completed".equalsIgnoreCase(row.getStatus())) {
                rideIds.add(row.getId());
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

    /**
     * Normalizes form values before validation or persistence.
     *
     * @param value raw request parameter value
     * @return a trimmed, non-null string
     */
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}