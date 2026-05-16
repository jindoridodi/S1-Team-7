<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%
String cp = request.getContextPath();
User currentUser = (User) session.getAttribute("currentUser");
String verificationMessage = request.getAttribute("verificationMessage") != null
    ? String.valueOf(request.getAttribute("verificationMessage"))
    : "Your driver registration is not active yet.";
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Driver access | UniRide</title>
  <link rel="stylesheet" href="<%= cp %>/assets/css/common.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/home.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/login.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/dashboard.css?v=20260427" />
</head>
<body>
  <div class="login-page dashboard-page">
    <nav class="navbar">
      <h1 class="logo"><a href="<%= cp %>/home">Uni<span class="highlight">Ride</span></a></h1>
      <div class="nav-links dashboard-nav-links">
        <span class="dashboard-welcome">Welcome <%= currentUser != null ? currentUser.getFirstName() : "Driver" %></span>
        <button type="button" class="nav-btn-secondary dashboard-role-switch-ui" aria-disabled="true" tabindex="-1">Switch to passenger</button>
        <a href="<%= cp %>/settings" class="nav-btn-primary dashboard-settings">Settings</a>
        <form method="post" action="<%= cp %>/logout" class="dashboard-inline-form">
          <button type="submit" class="nav-btn-secondary dashboard-signout">Sign Out</button>
        </form>
      </div>
    </nav>

    <div class="dashboard-main">
      <div class="dashboard-main-inner">
        <section class="dashboard-hero dashboard-section driver-gate-hero">
          <div class="dashboard-hero-copy driver-gate-copy">
            <h2>Driver dashboard locked</h2>
            <p><%= verificationMessage %></p>
            <div class="dashboard-actions u-mt-lg driver-gate-actions">
              <% if (currentUser != null && currentUser.hasRole("passenger")) { %>
                <a class="login-submit action-bookings u-inline-flex-center" href="<%= cp %>/dashboard/passenger">
                  Go to passenger dashboard
                </a>
              <% } %>
              <a class="nav-btn-secondary u-inline-flex-center" href="<%= cp %>/home" style="display:inline-flex;align-items:center;justify-content:center;padding:0.65rem 1.25rem;">Home</a>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</body>
</html>
