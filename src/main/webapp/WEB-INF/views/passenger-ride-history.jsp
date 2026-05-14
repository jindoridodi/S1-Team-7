<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User, java.time.LocalDateTime, java.time.format.DateTimeFormatter" %>
<%
String cp = request.getContextPath();
User currentUser = (User) session.getAttribute("currentUser");
java.util.List<model.UpcomingRide> rideHistory = (java.util.List<model.UpcomingRide>) request.getAttribute("rideHistory");
if (rideHistory == null) {
  rideHistory = java.util.Collections.emptyList();
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
        <span class="dashboard-welcome">Welcome <%= currentUser != null ? currentUser.getFirstName() : "Passenger" %></span>
        <a href="<%= cp %>/dashboard/passenger" class="nav-btn-secondary">Back to Dashboard</a>
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
            <p>Your past bookings (cancelled, declined, or completed).</p>
          </div>

          <% if (rideHistory.isEmpty()) { %>
            <div class="dashboard-empty">
              <p>No ride history yet.</p>
            </div>
          <% } else { %>
            <div class="ride-list">
              <% for (model.UpcomingRide ride : rideHistory) {
                   String rawDeparture = ride.getDepartureDate();
                   String formattedDeparture = rawDeparture;
                   String formattedTime = "";
                   try {
                     DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                     DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
                     DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
                     LocalDateTime dt = LocalDateTime.parse(rawDeparture, parser);
                     formattedDeparture = dt.format(dateFormatter);
                     formattedTime = dt.format(timeFormatter);
                   } catch (Exception ignored) {
                   }
              %>
                <div class="ride-item">
                  <div class="ride-row">
                    <div class="ride-info">
                      <strong><%= ride.getOrigin() %> → <%= ride.getDestination() %></strong><br />
                      <small class="ride-meta">Booking ID: #<%= ride.getBookingId() %></small>
                      <small class="ride-meta">Status: <%= ride.getBookingStatus() %></small>
                      <small class="ride-meta">Driver: <%= ride.getDriverName() != null ? ride.getDriverName() : "Not assigned" %></small>
                      <% if (ride.getDriverGender() != null && !ride.getDriverGender().isBlank()) { %>
                        <small class="ride-meta">Driver gender: <%= ride.getDriverGender() %></small>
                      <% } %>
                      <small class="ride-meta">Vehicle: <%= ride.getVehicleInfo() != null ? ride.getVehicleInfo() : "Not assigned" %></small>
                      <small class="ride-meta">Departure: <%= formattedDeparture %></small>
                      <% if (!formattedTime.isBlank()) { %>
                        <small class="ride-meta">Time: <%= formattedTime %></small>
                      <% } %>
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

