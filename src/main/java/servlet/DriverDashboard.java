package servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.User;
import store.AppStore;

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
        String status = AppStore.getDriverVerificationStatus(user.getEmail());

        if (status == null || !status.equalsIgnoreCase("verified")) {
        req.setAttribute("error", "Your driver account is pending verification.");
        req.getRequestDispatcher("/WEB-INF/views/driver-dashboard.jsp").forward(req, resp);
        return;
        }
        

        String action = safe(req.getParameter("action"));

        // Navigate to ride creation form if requested
        if ("showCreateRideForm".equals(action)) {
            req.setAttribute("vehicles", AppStore.getVehiclesForOwner(user.getEmail()));
            req.getRequestDispatcher("/WEB-INF/views/create-ride.jsp").forward(req, resp);
            return;
        }

        if ("showVehicles".equals(action)) {
            req.setAttribute("vehicles", AppStore.getVehiclesForOwner(user.getEmail()));
            req.getRequestDispatcher("/WEB-INF/views/driver-vehicles.jsp").forward(req, resp);
            return;
        }

        req.setAttribute("passengerRequests", AppStore.getPassengerRequestsForDriver(user.getEmail()));
        req.setAttribute("driverRides", AppStore.getRidesForDriver(user.getEmail()));
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
                    AppStore.createRide(user.getEmail(), vehicleId, origin, destination, sqlTimestamp, seats);
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
                        AppStore.addVehicle(user.getEmail(), make, model, color, plate, seats, insuranceNum);
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore invalid seat counts and fall through to the redirect.
                }
            }
        }

        if ("deleteVehicle".equals(action)) {
            String vehicleId = safe(req.getParameter("vehicleId"));
            if (!vehicleId.isBlank()) {
                AppStore.deleteVehicle(user.getEmail(), vehicleId);
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
                        AppStore.updateVehicle(user.getEmail(), vehicleId, make, model, color, plate, seats, insuranceNum);
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore invalid seat counts and fall through to the redirect.
                }
            }
        }

        if ("processPassengerRequest".equals(action)) {
            String bookingId = safe(req.getParameter("bookingId"));
            String decision = safe(req.getParameter("decision"));

            if (!bookingId.isBlank()) {
                String nextStatus = "";
                if ("accept".equalsIgnoreCase(decision)) {
                    nextStatus = "accepted";
                } else if ("decline".equalsIgnoreCase(decision)) {
                    nextStatus = "declined";
                }

                if (!nextStatus.isBlank()) {
                    AppStore.updatePassengerRequestStatus(user.getEmail(), bookingId, nextStatus);
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
                AppStore.cancelRide(user.getEmail(), rideId);
            }
        }

        resp.sendRedirect(req.getContextPath() + "/dashboard/driver");
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