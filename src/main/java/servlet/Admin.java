package servlet;

import repository.AdminRepository;
import repository.RideRepository;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/admin")
public class Admin extends HttpServlet {
    private static final String ADMIN_PASSWORD = "admin123";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // If not logged in as admin, show the login form.
        if (!isAdminSession(req)) {
            req.getRequestDispatcher("/WEB-INF/views/admin.jsp").forward(req, resp);
            return;
        }

        req.setAttribute("pendingDrivers", AdminRepository.getPendingDrivers());
        req.setAttribute("allRides", AdminRepository.getAllRidesForAdmin());
        req.getRequestDispatcher("/WEB-INF/views/admin.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String action = safe(req.getParameter("action"));

        // Handle admin login.
        if ("adminLogin".equals(action)) {
            String password = safe(req.getParameter("adminPassword"));
            if (ADMIN_PASSWORD.equals(password)) {
                req.getSession(true).setAttribute("isAdmin", true);
            } else {
                req.setAttribute("loginError", "Invalid admin password.");
                req.getRequestDispatcher("/WEB-INF/views/admin.jsp").forward(req, resp);
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/admin");
            return;
        }

        // All other actions require admin session.
        if (!isAdminSession(req)) {
            resp.sendRedirect(req.getContextPath() + "/admin");
            return;
        }

        if ("verifyDriver".equals(action)) {
            String userId = safe(req.getParameter("userId"));
            if (!userId.isBlank()) AdminRepository.verifyDriver(userId);
        }

        if ("rejectDriver".equals(action)) {
            String userId = safe(req.getParameter("userId"));
            if (!userId.isBlank()) AdminRepository.rejectDriver(userId);
        }

        if ("cancelRide".equals(action)) {
            String rideId = safe(req.getParameter("rideId"));
            if (!rideId.isBlank()) {
                RideRepository.cancelRideAsAdmin(rideId);
            }
        }

        if ("adminLogout".equals(action)) {
            req.getSession().removeAttribute("isAdmin");
        }

        resp.sendRedirect(req.getContextPath() + "/admin");
    }

    private boolean isAdminSession(HttpServletRequest req) {
        return Boolean.TRUE.equals(req.getSession(true).getAttribute("isAdmin"));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}