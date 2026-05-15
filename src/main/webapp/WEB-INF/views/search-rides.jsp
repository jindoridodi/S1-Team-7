<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User, java.time.LocalDateTime, java.time.format.DateTimeFormatter" %>
<%
String cp = request.getContextPath();
User currentUser = (User) session.getAttribute("currentUser");
String searchOrigin = (String) request.getAttribute("searchOrigin");
String searchDestination = (String) request.getAttribute("searchDestination");
String searchDate = (String) request.getAttribute("searchDate");
if (searchOrigin == null) searchOrigin = "";
if (searchDestination == null) searchDestination = "";
if (searchDate == null) searchDate = "";
boolean searchPerformed = Boolean.TRUE.equals(request.getAttribute("searchPerformed"));
java.util.List<model.Ride> availableRides = (java.util.List<model.Ride>) request.getAttribute("availableRides");
if (availableRides == null) availableRides = java.util.Collections.emptyList();
java.util.List<String[]> savedRoutes = (java.util.List<String[]>) request.getAttribute("savedRoutes");
if (savedRoutes == null) savedRoutes = java.util.Collections.emptyList();
String msg = (String) request.getAttribute("msg");
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Search Rides | UniRide</title>
  <link rel="stylesheet" href="<%= cp %>/assets/css/common.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/home.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/login.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/dashboard.css?v=20260427" />
  <style>
    .ride-item--highlight {
      border-left: 3px solid #f5a623;
      background: rgba(245, 166, 35, 0.05);
    }
  </style>
</head>
<body>
  <div class="login-page dashboard-page">
    <nav class="navbar">
      <h1 class="logo"><a href="<%= cp %>/home">Uni<span class="highlight">Ride</span></a></h1>
      <div class="nav-links dashboard-nav-links">
        <span class="dashboard-welcome">Welcome <%= currentUser != null ? currentUser.getFirstName() : "Passenger" %></span>
        <a href="<%= cp %>/dashboard/passenger" class="nav-btn-secondary">Dashboard</a>
        <form method="post" action="<%= cp %>/logout" class="dashboard-inline-form">
          <button type="submit" class="nav-btn-secondary dashboard-signout">Sign Out</button>
        </form>
      </div>
    </nav>

    <div class="dashboard-main">
      <div class="dashboard-main-inner">

        <% if ("seatRequested".equals(msg)) { %>
          <p class="signup-success">Your seat request was submitted. Check Upcoming Rides on your dashboard.</p>
        <% } else if ("seatRequestFailed".equals(msg)) { %>
          <p style="color:#f87171;text-align:center;">Could not request that seat. The ride may be full or no longer available.</p>
        <% } %>
        <% if (request.getAttribute("error") != null) { %>
          <p style="color:#f87171;text-align:center;"><%= request.getAttribute("error") %></p>
        <% } %>

        <section class="dashboard-section dashboard-passenger-section">
          <div class="dashboard-section-heading">
            <h3>Search rides</h3>
            <p>Find open trips posted by drivers. Searching only shows results — you choose when to request a seat.</p>
          </div>

          <form method="get" action="<%= cp %>/dashboard/passenger" class="dashboard-form-grid dashboard-search-form">
            <input type="hidden" name="action" value="searchRides" />
            <input type="text" name="searchOrigin" class="login-input" placeholder="From (origin)" value="<%= searchOrigin %>" />
            <input type="text" name="searchDestination" class="login-input" placeholder="To (destination)" value="<%= searchDestination %>" />
            <input type="date" name="searchDate" class="login-input" value="<%= searchDate %>" />
            <button type="submit" class="login-submit action-find u-w-100">Search rides</button>
          </form>
        </section>

        <% if (searchPerformed) { %>
          <section class="dashboard-section dashboard-passenger-section">
            <div class="dashboard-section-heading">
              <h3>Search results</h3>
              <p><%= availableRides.isEmpty() ? "No matching rides right now." : availableRides.size() + " ride(s) match your search." %></p>
            </div>

            <% if (availableRides.isEmpty()) { %>
              <div class="dashboard-empty">
                <p>No active rides match those details. Try different filters or post an open request for drivers.</p>
              </div>
            <% } else { %>
              <div class="ride-list ride-list--spaced">
                <% for (model.Ride ride : availableRides) {
                     boolean isMatch = false;
                     for (String[] sr : savedRoutes) {
                       if (ride.getOrigin().toLowerCase().contains(sr[1].toLowerCase()) &&
                           ride.getDestination().toLowerCase().contains(sr[2].toLowerCase())) {
                         isMatch = true;
                         break;
                       }
                     }
                     String rawDeparture = ride.getDepartureDate();
                     String formattedDeparture = rawDeparture;
                     String formattedTime = "";
                     try {
                       DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                       LocalDateTime dt = LocalDateTime.parse(rawDeparture, parser);
                       formattedDeparture = dt.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
                       formattedTime = dt.format(DateTimeFormatter.ofPattern("h:mm a"));
                     } catch (Exception ignored) {
                     }
                %>
                  <div class="ride-item <%= isMatch ? "ride-item--highlight" : "" %>">
                    <div class="ride-row">
                      <div class="ride-info">
                        <strong><%= ride.getOrigin() %> &rarr; <%= ride.getDestination() %></strong>
                        <% if (isMatch) { %><small class="ride-meta" style="color:#f5a623;">&#9733; Matches your saved route</small><% } %>
                        <small class="ride-meta">Driver: <%= ride.getDriverName() != null ? ride.getDriverName() : "Not assigned" %></small>
                        <% if (ride.getDriverGender() != null && !ride.getDriverGender().isBlank()) { %>
                          <small class="ride-meta">Driver gender: <%= ride.getDriverGender() %></small>
                        <% } %>
                        <small class="ride-meta">Vehicle: <%= ride.getVehicleInfo() != null ? ride.getVehicleInfo() : "Not assigned" %></small>
                        <small class="ride-meta">Departure: <%= formattedDeparture %><% if (!formattedTime.isBlank()) { %> at <%= formattedTime %><% } %></small>
                        <% if (ride.getSeatsLeft() == 0) { %>
                          <small class="ride-meta ride-meta--danger">Full</small>
                        <% } else if (ride.getSeatsLeft() <= 2) { %>
                          <small class="ride-meta ride-meta--warn">Few seats left: <%= ride.getSeatsLeft() %></small>
                        <% } else { %>
                          <small class="ride-meta ride-meta--success">Seats left: <%= ride.getSeatsLeft() %></small>
                        <% } %>
                      </div>
                      <div class="ride-actions">
                        <% if (ride.getSeatsLeft() > 0 && "open".equalsIgnoreCase(ride.getStatus())) { %>
                          <a href="<%= cp %>/dashboard/passenger?action=confirmSeatRequest&amp;rideId=<%= ride.getId() %>"
                             class="request-approve ride-request-link">Request seat</a>
                        <% } else { %>
                          <span class="ride-meta">Not available</span>
                        <% } %>
                      </div>
                    </div>
                  </div>
                <% } %>
              </div>
            <% } %>

            <p class="dashboard-request-ride-link">
              Can't find a ride?
              <a href="<%= cp %>/dashboard/passenger?action=showRequestRideForm">Post an open request for drivers</a>
            </p>
          </section>
        <% } %>

      </div>
    </div>
  </div>
</body>
</html>
