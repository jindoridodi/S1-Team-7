<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%
String cp = request.getContextPath();
User currentUser = (User) session.getAttribute("currentUser");
boolean canSwapDashboard = currentUser != null && currentUser.hasRole("driver") && currentUser.hasRole("passenger");
java.util.List<String[]> savedRoutes = (java.util.List<String[]>) request.getAttribute("savedRoutes");
if (savedRoutes == null) savedRoutes = java.util.Collections.emptyList();
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>My Saved Routes | UniRide</title>
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
        <span class="dashboard-welcome">Welcome <%= currentUser != null ? currentUser.getFirstName() : "Passenger" %></span>
        <a href="<%= cp %>/dashboard/passenger" class="nav-btn-secondary">Back to Dashboard</a>
        <form method="post" action="<%= cp %>/logout" class="dashboard-inline-form">
          <button type="submit" class="nav-btn-secondary dashboard-signout">Sign Out</button>
        </form>
      </div>
    </nav>

    <div class="dashboard-main">
      <div class="dashboard-main-inner">

        <section class="dashboard-hero dashboard-section">
          <div class="dashboard-hero-copy">
            <span class="campus-tag">My Saved Routes</span>
            <h2>Manage your favorite routes</h2>
            <p>
              Save your frequent routes and we'll highlight matching rides for you on the dashboard.
            </p>
          </div>
        </section>

        <section class="dashboard-section dashboard-passenger-section">
          <div class="dashboard-section-heading">
            <h3>Your Saved Routes</h3>
            <p><%= savedRoutes.isEmpty() ? "You haven't saved any routes yet. Add one below!" : "You have " + savedRoutes.size() + " saved route" + (savedRoutes.size() == 1 ? "" : "s") + "." %></p>
          </div>

          <% if (!savedRoutes.isEmpty()) { %>
            <div class="ride-list" style="margin-bottom: 1rem;">
              <% for (String[] route : savedRoutes) { %>
                <div class="ride-item">
                  <div class="ride-row">
                    <div class="ride-info">
                      <strong><%= route[1] %> &rarr; <%= route[2] %></strong>
                    </div>
                    <form method="post" action="<%= cp %>/dashboard/passenger" class="dashboard-inline-form">
                      <input type="hidden" name="action" value="deleteRoute" />
                      <input type="hidden" name="routeId" value="<%= route[0] %>" />
                      <button type="submit" class="request-decline">Remove</button>
                    </form>
                  </div>
                </div>
              <% } %>
            </div>
          <% } %>

          <div style="padding-top: 2rem; border-top: 1px solid #ddd;">
            <h4 style="margin-bottom: 1rem;">Add a New Route</h4>
            <form method="post" action="<%= cp %>/dashboard/passenger" class="dashboard-form-grid">
              <input type="hidden" name="action" value="saveRoute" />
              <input type="text" name="startLocation" placeholder="From (e.g. Fremont)" required />
              <input type="text" name="endLocation" placeholder="To (e.g. SJSU)" required />
              <button type="submit" class="login-submit">Save Route</button>
            </form>
          </div>
        </section>

      </div>
    </div>
  </div>
</body>
</html>
