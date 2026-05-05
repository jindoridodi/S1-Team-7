package servlet;

import model.User;
import repository.PasswordUtil;
import repository.UserRepository;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import at.favre.lib.crypto.bcrypt.BCrypt;

import java.io.IOException;

/**
 * Serves the account settings workflow for any authenticated UniRide user.
 */
@WebServlet("/settings")
public class Settings extends HttpServlet {
    /**
     * Loads the settings view only for signed-in users.
     *
     * @param req current request containing the authenticated session, if any
     * @param resp response used to redirect unauthenticated users
     * @throws ServletException if request dispatching fails
     * @throws IOException if the redirect cannot be written
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // The page is account-specific, so unauthenticated users are sent back to login.
        User user = (User) req.getSession(true).getAttribute("currentUser");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/settings.jsp").forward(req, resp);
    }

    /**
     * Validates the password change form and updates the stored credentials.
     *
     * <p>The servlet keeps validation server-side so the password policy cannot
     * be bypassed by submitting a custom request. Any validation error is stored
     * on the request and echoed back by the JSP.</p>
     *
     * @param req current form submission containing the password fields
     * @param resp response used to redirect unauthenticated users
     * @throws ServletException if request dispatching fails
     * @throws IOException if the redirect or forward cannot be written
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Re-check the session on submit so password changes cannot be posted anonymously.
        User user = (User) req.getSession(true).getAttribute("currentUser");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // Trim user input up front so validation logic sees the same values the browser submitted.
        String currentPassword = safe(req.getParameter("currentPassword"));
        String newPassword     = safe(req.getParameter("newPassword"));
        String confirmPassword = safe(req.getParameter("confirmPassword"));

        // Current password must match the one already stored for the active account.
        boolean currentVerified = BCrypt.verifyer()
            .verify(currentPassword.toCharArray(), user.getPassword())
            .verified;
        if (!currentVerified) {
            req.setAttribute("errorCurrentPassword", "Current password is incorrect.");
        }

        // Apply the password policy before attempting the database update.
        if (newPassword.isBlank()) {
            req.setAttribute("errorNewPassword", "New password is required.");
        } else if (!PasswordUtil.isStrongPassword(newPassword)) {
            req.setAttribute("errorNewPassword",
                "Password must be at least 8 characters and include 1 uppercase letter, 1 lowercase letter, and 1 number.");
        }

        // Reusing the old password would create the illusion of a successful update.
        boolean newMatchesOld = BCrypt.verifyer()
            .verify(newPassword.toCharArray(), user.getPassword())
            .verified;
        if (newMatchesOld) {
            req.setAttribute("errorNewPassword", "New password cannot be the same as current password.");
        }

        // Confirmation is checked server-side to keep the browser and servlet in agreement.
        if (!newPassword.equals(confirmPassword)) {
            req.setAttribute("errorConfirmPassword", "Passwords do not match.");
        }

        // Preserve the entered state and return the JSP when validation fails.
        if (hasErrors(req)) {
            req.getRequestDispatcher("/WEB-INF/views/settings.jsp").forward(req, resp);
            return;
        }

        // Persist the change only after all validation passes.
        if (UserRepository.updatePassword(user.getEmail(), newPassword)) {
            req.setAttribute("successMessage", "Password changed successfully.");
            // Reload the user because the session object is immutable and may now be stale.
            User updated = UserRepository.authenticate(user.getEmail(), newPassword);
            if (updated != null) {
                req.getSession(true).setAttribute("currentUser", updated);
            }
            req.getRequestDispatcher("/WEB-INF/views/settings.jsp").forward(req, resp);
        } else {
            req.setAttribute("errorNewPassword", "Failed to update password.");
            req.getRequestDispatcher("/WEB-INF/views/settings.jsp").forward(req, resp);
        }
    }

    /**
     * Normalizes request parameters so validation code can safely call string
     * methods without repeating null checks.
     *
     * @param s raw request parameter value
     * @return a trimmed, non-null string
     */
    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Detects whether any validation message was attached to the current request.
     *
     * @param req request carrying field-specific validation errors
     * @return {@code true} when the form should be re-rendered with messages
     */
    private boolean hasErrors(HttpServletRequest req) {
        return req.getAttribute("errorCurrentPassword") != null ||
               req.getAttribute("errorNewPassword") != null ||
               req.getAttribute("errorConfirmPassword") != null;
    }
}
