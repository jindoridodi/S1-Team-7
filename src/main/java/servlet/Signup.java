package servlet;

import model.User;
import repository.PasswordUtil;
import repository.UserRepository;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@WebServlet("/signup")
/**
 * Handles account registration for users who may join as passengers, drivers,
 * or both.
 *
 * The servlet performs all validation server-side so the JSP can simply echo
 * request-scoped errors and preserve the user's input when the form is reloaded.
 */
public class Signup extends HttpServlet {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    /** Campus accounts only: local-part @ literal domain sjsu.edu (email is normalized to lowercase before match). */
    private static final Pattern SJSU_EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@sjsu\\.edu$");
    private static final Pattern SJSU_ID_PATTERN = Pattern.compile("^\\d{9}$");

    /**
     * Renders the signup form.
     *
     * @param req current request
     * @param resp response used to forward to the JSP
     * @throws ServletException if the JSP dispatch fails
     * @throws IOException if the response cannot be written
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/signup.jsp").forward(req, resp);
    }

    /**
     * Validates the signup form, creates the account, and returns feedback to the JSP.
     *
     * @param req current form submission
     * @param resp response used to forward back to the form
     * @throws ServletException if the JSP dispatch fails
     * @throws IOException if the response cannot be written
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Normalize once so all validation and persistence sees the same values.
        String firstName     = safe(req.getParameter("firstName"));
        String lastName      = safe(req.getParameter("lastName"));
        String email         = safe(req.getParameter("email")).toLowerCase();
        String sjsuId        = safe(req.getParameter("sjsuId"));
        String gender        = safe(req.getParameter("gender"));
        String password      = safe(req.getParameter("password"));
        String licenseNumber = safe(req.getParameter("licenseNumber"));
        String[] rolesArray  = req.getParameterValues("roles");

        // Leave form state untouched until validation decides whether the request should bounce back.

        if (firstName.isBlank())  req.setAttribute("errorFirstName", "First name is required.");
        if (lastName.isBlank())   req.setAttribute("errorLastName",  "Last name is required.");

        if (email.isBlank()) {
            req.setAttribute("errorEmail", "Email is required.");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            req.setAttribute("errorEmail", "Enter a valid email address.");
        } else if (!SJSU_EMAIL_PATTERN.matcher(email).matches()) {
            req.setAttribute("errorEmail", "Registration requires an @sjsu.edu email address.");
        } else if (UserRepository.hasUser(email)) {
            req.setAttribute("errorEmail", "This email is already registered.");
        }

        if (!SJSU_ID_PATTERN.matcher(sjsuId).matches()) {
            req.setAttribute("errorSjsuId", "SJSU ID must be 9 digits.");
        }
        if (gender.isBlank()) req.setAttribute("errorGender", "Please select a gender.");

        if (password.isBlank()) {
            req.setAttribute("errorPassword", "Password is required.");
        } else if (!PasswordUtil.isStrongPassword(password)) {
            req.setAttribute("errorPassword",
                "Password must be at least 8 characters and include 1 uppercase letter, 1 lowercase letter, and 1 number.");
        }

        Set<String> roles = new HashSet<>();
        if (rolesArray != null) {
            for (String role : rolesArray) {
                if ("driver".equals(role) || "passenger".equals(role)) roles.add(role);
            }
        }
        if (roles.isEmpty()) {
            req.setAttribute("errorRoles", "Please choose at least one registration type.");
        }

        if (roles.contains("driver") && licenseNumber.isBlank()) {
            req.setAttribute("errorLicense", "License number is required for drivers.");
        }

        if (hasErrors(req)) {
            // Preserve entered values so the user can correct errors without retyping everything.
            req.setAttribute("firstName",     firstName);
            req.setAttribute("lastName",      lastName);
            req.setAttribute("email",         email);
            req.setAttribute("sjsuId",        sjsuId);
            req.setAttribute("gender",        gender);
            req.setAttribute("licenseNumber", licenseNumber);
            // Preserve selected roles so checkboxes remain checked on error
            if (rolesArray != null) req.setAttribute("roles", rolesArray);
            req.getRequestDispatcher("/WEB-INF/views/signup.jsp").forward(req, resp);
            return;
        }

        User user = UserRepository.createUser(firstName, lastName, email, sjsuId, gender, password, roles, licenseNumber);
        if (user == null) {
            // Keep entered values when reporting duplicate-email failure so the user only changes the conflicting field.
            req.setAttribute("firstName",     firstName);
            req.setAttribute("lastName",      lastName);
            req.setAttribute("email",         email);
            req.setAttribute("sjsuId",        sjsuId);
            req.setAttribute("gender",        gender);
            req.setAttribute("licenseNumber", licenseNumber);
            if (rolesArray != null) req.setAttribute("roles", rolesArray);
            req.setAttribute("errorEmail", "This email is already registered.");
            req.getRequestDispatcher("/WEB-INF/views/signup.jsp").forward(req, resp);
            return;
        }

        req.setAttribute("successMessage", "Registration complete. Your account has been created.");
        // Clear role selections on success so the rendered form does not imply a retry state.
        req.setAttribute("roles", new String[0]);
        req.getRequestDispatcher("/WEB-INF/views/signup.jsp").forward(req, resp);
    }
    

    /**
     * Detects whether any validation errors were attached to the request.
     *
     * @param req request containing field-specific validation attributes
     * @return true when the form should be re-rendered with feedback
     */
    private boolean hasErrors(HttpServletRequest req) {
        return req.getAttribute("errorFirstName") != null
            || req.getAttribute("errorLastName")  != null
            || req.getAttribute("errorEmail")     != null
            || req.getAttribute("errorSjsuId")    != null
            || req.getAttribute("errorGender")    != null
            || req.getAttribute("errorPassword")  != null
            || req.getAttribute("errorRoles")     != null
            || req.getAttribute("errorLicense")   != null;
    }

    /**
     * Normalizes request parameters so validation can safely inspect them.
     *
     * @param value raw request parameter value
     * @return a trimmed, non-null string
     */
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
