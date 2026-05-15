package servlet;

import model.User;
import repository.UserRepository;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Handles login form rendering and session-based authentication.
 */
@WebServlet("/login")
public class Login extends HttpServlet {
    /** Campus accounts only: local-part @ literal domain sjsu.edu (email is normalized to lowercase before match). */
    private static final Pattern SJSU_EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@sjsu\\.edu$");
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Normalize and trim to avoid duplicate accounts based on casing/spacing.
        String email = safe(req.getParameter("email")).toLowerCase();
        String password = safe(req.getParameter("password"));

        req.setAttribute("email", email);

        if (email.isBlank()) {
            req.setAttribute("errorEmail", "Email is required.");
        } else if (!SJSU_EMAIL_PATTERN.matcher(email).matches()) {
            req.setAttribute("errorEmail", "Sign in requires an @sjsu.edu email address.");
        }

        if (password.isBlank()) {
            req.setAttribute("errorPassword", "Password is required.");
        }

        if (req.getAttribute("errorEmail") != null || req.getAttribute("errorPassword") != null) {
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
            return;
        }

        User user = UserRepository.authenticate(email, password);
        if (user == null) {
            req.setAttribute("errorPassword", "Incorrect email or password.");
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
            return;
        }

        // Persist the authenticated user in session for downstream guards.
        req.getSession(true).setAttribute("currentUser", user);

        String redirect = safe(req.getParameter("redirect"));
        if (!redirect.isBlank() && redirect.startsWith(req.getContextPath())) {
            resp.sendRedirect(redirect);
            return;
        }

        resp.sendRedirect(req.getContextPath() + UserRepository.primaryDashboardPathRelative(user));
    }

    /**
     * Null-safe trim helper for request parameters.
     */
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
