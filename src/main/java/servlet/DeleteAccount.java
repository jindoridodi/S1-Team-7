package servlet;

import model.User;
import repository.UserRepository;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Deletes the logged-in account and clears the active session.
 *
 * The account is soft-deleted in the database so the record can remain for
 * history and integrity while the user is immediately signed out.
 */
@WebServlet("/delete-account")
public class DeleteAccount extends HttpServlet {
    /**
     * Marks the current account deleted when available and always ends the session.
     *
     * @param req current request carrying the authenticated session
     * @param resp response used to redirect to the public home page
     * @throws IOException if the redirect cannot be written
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession(true).getAttribute("currentUser");
        if (user != null) {
            UserRepository.deleteUser(user.getEmail());
        }
        // Always invalidate, even if no user was found, to enforce sign-out.
        req.getSession(true).invalidate();
        resp.sendRedirect(req.getContextPath() + "/home");
    }
}
