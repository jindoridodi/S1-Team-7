package servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import model.User;
import repository.UserRepository;

import java.io.IOException;

/**
 * Renders the public landing page.
 */
@WebServlet("/home")
public class Home extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession sess = req.getSession(false);
        User currentUser = sess == null ? null : (User) sess.getAttribute("currentUser");
        String dashboardPath = req.getContextPath() + "/login";
        if (currentUser != null) {
            dashboardPath = req.getContextPath() + UserRepository.primaryDashboardPathRelative(currentUser);
        }
        req.setAttribute("dashboardPath", dashboardPath);
        req.getRequestDispatcher("/WEB-INF/views/home.jsp").forward(req, resp);
    }
}
