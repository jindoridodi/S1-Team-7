<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User, java.time.LocalDateTime, java.time.format.DateTimeFormatter" %>
<%
String cp = request.getContextPath();
User currentUser = (User) session.getAttribute("currentUser");
java.util.List<model.Ride> driverRideHistory = (java.util.List<model.Ride>) request.getAttribute("driverRideHistory");
if (driverRideHistory == null) {
  driverRideHistory = java.util.Collections.emptyList();
}
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Ride History | UniRide</title>
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
        <a href="<%= cp %>/dashboard/driver" class="nav-btn-secondary">Back to Dashboard</a>
        <form method="post" action="<%= cp %>/logout" class="dashboard-inline-form">
          <button type="submit" class="nav-btn-secondary dashboard-signout">Sign Out</button>
        </form>
      </div>
    </nav>

    <div class="dashboard-main">
      <div class="dashboard-main-inner">
        <section class="dashboard-section dashboard-passenger-section">
          <div class="dashboard-section-heading">
            <h3>Ride History</h3>
            <p>Past and completed trips you have offered (including cancelled rides).</p>
          </div>

          <% if (driverRideHistory.isEmpty()) { %>
            <div class="dashboard-empty">
              <p>No ride history yet.</p>
            </div>
          <% } else { %>
            <div class="ride-list">
              <% for (model.Ride ride : driverRideHistory) {
                   String rawDep = ride.getDepartureDate();
                   String fmtDep = rawDep;
                   String fmtTime = "";
                   try {
                     DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                     LocalDateTime dt = LocalDateTime.parse(rawDep, parser);
                     fmtDep = dt.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
                     fmtTime = dt.format(DateTimeFormatter.ofPattern("h:mm a"));
                   } catch (Exception ignored) {
                   }
                   String rawStatus = ride.getStatus();
                   String statusLabel = "";
                   if (rawStatus != null && !rawStatus.isBlank()) {
                     statusLabel = rawStatus.substring(0, 1).toUpperCase() + rawStatus.substring(1).toLowerCase();
                   }
              %>
                <div class="ride-item">
                  <div class="ride-row">
                    <div class="ride-info">
                      <strong><%= ride.getOrigin() %> → <%= ride.getDestination() %></strong><br />
                      <small class="ride-meta">Ride ID: #<%= ride.getId() %></small>
                      <small class="ride-meta">Status: <%= statusLabel %></small>
                      <small class="ride-meta">Vehicle: <%= ride.getVehicleInfo() != null ? ride.getVehicleInfo() : "—" %></small>
                      <small class="ride-meta">Departure: <%= fmtDep %></small>
                      <% if (!fmtTime.isBlank()) { %>
                        <small class="ride-meta">Time: <%= fmtTime %></small>
                      <% } %>
                      <small class="ride-meta">Seats remaining (at snapshot): <%= ride.getSeatsLeft() %></small>
                    </div>
                  </div>
                </div>
              <% } %>
            </div>
          <% } %>
        </section>
      </div>
    </div>
  </div>
</body>
</html>
