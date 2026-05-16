<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%
// Pull the context path once so every server-generated URL stays portable across deployments.
String cp = request.getContextPath();
// The authenticated user is stored in the session by the login flow and reused here for display only.
User currentUser = (User) session.getAttribute("currentUser");
Object dashboardAttr = request.getAttribute("dashboardPath");
String dashboardPath = dashboardAttr instanceof String ? (String) dashboardAttr : (cp + "/dashboard/passenger");
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Settings | UniRide</title>
  <link rel="stylesheet" href="<%= cp %>/assets/css/common.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/home.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/login.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/dashboard.css?v=20260427" />
</head>
<body>
  <div class="login-page dashboard-page">
    <nav class="navbar">
      <!-- The header reuses dashboard navigation styles so settings feels part of the same logged-in shell. -->
      <h1 class="logo"><a href="<%= cp %>/home">Uni<span class="highlight">Ride</span></a></h1>
      <div class="nav-links dashboard-nav-links">
        <span class="dashboard-welcome">Welcome <%= currentUser != null ? currentUser.getFirstName() : "User" %></span>
        <a href="<%= dashboardPath %>" class="nav-btn-secondary">Back to Dashboard</a>
        <form method="post" action="<%= cp %>/logout" class="dashboard-inline-form">
          <button type="submit" class="nav-btn-secondary dashboard-signout">Sign Out</button>
        </form>
      </div>
    </nav>

    <div class="dashboard-main">
      <div class="dashboard-main-inner">
        <section class="dashboard-hero dashboard-section settings-hero">
          <div class="dashboard-hero-copy">
            <span class="campus-tag">Account Settings</span>
            <h2>Keep your account secure and up to date.</h2>
            <p>
              Review your profile details, update your password, and manage your UniRide account from one place.
            </p>
          </div>
          <div class="settings-quick-card">
            <h3>Current account</h3>
            <p><strong>Email</strong><span><%= currentUser != null ? currentUser.getEmail() : "" %></span></p>
            <p><strong>Name</strong><span><%= currentUser != null ? currentUser.getFirstName() + " " + currentUser.getLastName() : "" %></span></p>
          </div>
        </section>

        <section class="dashboard-section settings-full">
          <div class="settings-panel">
            <!-- Request-scoped messages are rendered here so the POST handler can redirect users back with feedback. -->
            <% if (request.getAttribute("successMessage") != null) { %>
              <div class="form-success settings-message">
                <%= request.getAttribute("successMessage") %>
              </div>
            <% } %>

            <!-- The profile fields are intentionally disabled because this page only supports password changes. -->
            <form id="passwordForm" method="post" action="<%= cp %>/settings" class="settings-form">
              <hr />

              <h3>Change Password</h3>

              <div class="form-group">
                <label for="currentPassword">Current Password</label>
                <input type="password" id="currentPassword" name="currentPassword" class="login-input" required />
                <!-- Server-side validation errors are surfaced inline so the user can correct the exact field. -->
                <% if (request.getAttribute("errorCurrentPassword") != null) { %>
                  <span class="form-error"><%= request.getAttribute("errorCurrentPassword") %></span>
                <% } %>
              </div>

              <div class="form-group">
                <label for="newPassword">New Password</label>
                <input type="password" id="newPassword" name="newPassword" class="login-input" required />
                <small>Must be at least 8 characters long.</small>
                <% if (request.getAttribute("errorNewPassword") != null) { %>
                  <span class="form-error"><%= request.getAttribute("errorNewPassword") %></span>
                <% } %>
              </div>

              <div class="form-group">
                <label for="confirmPassword">Confirm New Password</label>
                <input type="password" id="confirmPassword" name="confirmPassword" class="login-input" required />
                <% if (request.getAttribute("errorConfirmPassword") != null) { %>
                  <span class="form-error"><%= request.getAttribute("errorConfirmPassword") %></span>
                <% } %>
              </div>

              <button type="submit" class="login-submit settings-update-btn">Update Password</button>
            </form>

            <div class="dashboard-danger-wrap settings-danger-wrap">
              <h3>Delete Account</h3>
              <p>
                This permanently removes your account and all related access. You will need to create a new account to use UniRide again.
              </p>
              <form method="post" action="<%= cp %>/delete-account"
                    onsubmit="return confirm('Are you sure you want to delete your account?');">
                <button type="submit" class="dashboard-danger-btn">Delete Account</button>
              </form>
            </div>
          </div>
        </section>

        <% // When the form returns with feedback, bring the form back into view so the message is visible immediately. %>
        <% boolean hasMsgs = request.getAttribute("successMessage") != null ||
                          request.getAttribute("errorCurrentPassword") != null ||
                          request.getAttribute("errorNewPassword") != null ||
                          request.getAttribute("errorConfirmPassword") != null; %>

        <% if (hasMsgs) { %>
          <script>
            // The scroll is purely presentational; it helps users see validation feedback after a POST/forward.
            document.addEventListener('DOMContentLoaded', function() {
              var el = document.getElementById('passwordForm');
              if (el) {
                el.scrollIntoView({ behavior: 'auto', block: 'center' });
              }
            });
          </script>
        <% } %>
      </div>
    </div>
  </div>
</body>
</html>
