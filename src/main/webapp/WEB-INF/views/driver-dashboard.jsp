<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="model.PassengerRequest" %>
<%@ page import="model.User" %>
<%@ page import="model.Vehicle" %>
<%
String cp = request.getContextPath();
User currentUser = (User) session.getAttribute("currentUser");
boolean driverVerified = Boolean.TRUE.equals(request.getAttribute("driverVerified"));
List<PassengerRequest> passengerRequests = (List<PassengerRequest>) request.getAttribute("passengerRequests");
if (passengerRequests == null) {
  passengerRequests = java.util.Collections.emptyList();
}
@SuppressWarnings("unchecked")
List<Vehicle> driverVehicles = (List<Vehicle>) request.getAttribute("driverVehicles");
if (driverVehicles == null) {
  driverVehicles = java.util.Collections.emptyList();
}
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Driver Dashboard | UniRide</title>
  <link rel="stylesheet" href="<%= cp %>/assets/css/common.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/home.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/login.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/dashboard.css?v=20260427" />
  <style>
  </style>
</head>
<body>
  <div class="login-page dashboard-page">
    <nav class="navbar">
      <h1 class="logo"><a href="<%= cp %>/home">Uni<span class="highlight">Ride</span></a></h1>
      <div class="nav-links dashboard-nav-links">
        <span class="dashboard-welcome">Welcome <%= currentUser != null ? currentUser.getFirstName() : "Driver" %></span>
        <% if (driverVerified) { %>
          <a href="<%= cp %>/dashboard/passenger" class="nav-btn-secondary dashboard-role-switch-ui">Switch to Passenger</a>
        <% } %>
        <a href="<%= cp %>/settings" class="nav-btn-primary dashboard-settings">Settings</a>
        <form method="post" action="<%= cp %>/logout" class="dashboard-inline-form">
          <button type="submit" class="nav-btn-secondary dashboard-signout">Sign Out</button>
        </form>
      </div>
    </nav>
    <% if (request.getAttribute("error") != null) { %>
  <p style="color:red; text-align:center; margin-top:10px;">
    <%= request.getAttribute("error") %>
  </p>
<% } %>

    <div class="dashboard-main">
      <div class="dashboard-main-inner">

          <section class="dashboard-hero dashboard-section">
            <div class="dashboard-hero-copy">
              <span class="campus-tag">Driver Hub</span>
              <h2>Manage your rides and vehicles from one place.</h2>
              <p>
                Keep your car details updated, review driver tools, and stay ready for the next ride request.
              </p>
            </div>
            <div class="dashboard-actions">
              <a href="<%= cp %>/dashboard/driver?action=showCreateRideForm" class="login-submit action-create u-inline-flex-center">
                Create a Ride
              </a>
              <a href="<%= cp %>/dashboard/driver?action=showVehicles" class="login-submit action-requests u-inline-flex-center">
                My Vehicles
              </a>
              <a href="<%= cp %>/dashboard/driver?action=showRideHistory" class="login-submit action-bookings u-inline-flex-center">
                Ride History
              </a>
            </div>
          </section>

          <section class="dashboard-section dashboard-vehicle-card">
            <div class="dashboard-section-heading">
              <h3>My Rides</h3>
              <p>Upcoming rides you are hosting. Cancel a ride to remove all passengers and notify them. Past trips are in Ride History.</p>
            </div>

            <%
              java.util.List<model.Ride> driverRides = (java.util.List<model.Ride>) request.getAttribute("driverRides");
              if (driverRides == null) driverRides = java.util.Collections.emptyList();
            %>

            <% if (driverRides.isEmpty()) { %>
              <div class="dashboard-empty">
                <p>No upcoming rides. Create one or check Ride History for past trips.</p>
              </div>
            <% } else { %>
              <div class="ride-list">
                <% for (model.Ride ride : driverRides) {
                     String rawDep = ride.getDepartureDate();
                     String fmtDep = rawDep;
                     String fmtTime = "";
                     try {
                       java.time.LocalDateTime dt = java.time.LocalDateTime.parse(rawDep,
                           java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                       fmtDep  = dt.format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"));
                       fmtTime = dt.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"));
                     } catch (Exception ignored) {}
                %>
                  <div class="ride-item">
                    <div class="ride-row">
                      <div class="ride-info">
                        <strong><%= ride.getOrigin() %> &rarr; <%= ride.getDestination() %></strong>
                        <small class="ride-meta">Date: <%= fmtDep %> <%= fmtTime %></small>
                        <small class="ride-meta">Seats left: <%= ride.getSeatsLeft() %></small>
                        <% if ("completed".equalsIgnoreCase(ride.getStatus())) { %>
                        <small class="ride-meta">Status: Completed</small>
                      <% } else if ("cancelled".equalsIgnoreCase(ride.getStatus())) { %>
                        <small class="ride-meta">Status: Cancelled</small>
                      <% } else { %>
                        <small class="ride-meta">Status: Active</small>
                      <% } %>
                      </div>
                      <% if (!"completed".equalsIgnoreCase(ride.getStatus()) &&
                        !"cancelled".equalsIgnoreCase(ride.getStatus())) { %>
                    <div class="ride-actions">
                      <form method="post" action="<%= cp %>/dashboard/driver"
                            class="dashboard-inline-form"
                            onsubmit="return confirm('Cancel this ride? All passengers will be notified.');">
                        <input type="hidden" name="action"  value="cancelRide" />
                        <input type="hidden" name="rideId"  value="<%= ride.getId() %>" />
                        <button type="submit" class="request-decline">Cancel Ride</button>
                      </form>
                    </div>
                  <% } %>
                    </div>
                  </div>
                <% } %>
              </div>
            <% } %>
          </section>

          <section class="dashboard-section dashboard-vehicle-card">
            <div class="dashboard-section-heading">
              <h3>Passenger Requests</h3>
              <p>Incoming requests appear here so you can review them directly from your dashboard.</p>
            </div>

            <% if (passengerRequests.isEmpty()) { %>
              <div class="dashboard-empty">
                <p>No passenger requests yet.</p>
              </div>
            <% } else { %>
              <div class="ride-list">
                <% for (PassengerRequest requestRow : passengerRequests) {
                     String rawDeparture = requestRow.getDepartureDate();
                     String formattedDeparture = rawDeparture;
                     String formattedDepartureTime = "";
                     try {
                       DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                       LocalDateTime departureDt = LocalDateTime.parse(rawDeparture, parser);
                       formattedDeparture = departureDt.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
                       formattedDepartureTime = departureDt.format(DateTimeFormatter.ofPattern("h:mm a"));
                     } catch (Exception ignored) {
                     }

                     String rawRequestedAt = requestRow.getBookingTimestamp();
                     String formattedRequestedAt = rawRequestedAt;
                     try {
                       DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                       LocalDateTime requestedDt = LocalDateTime.parse(rawRequestedAt, parser);
                       formattedRequestedAt = requestedDt.format(DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a"));
                     } catch (Exception ignored) {
                     }
                %>
                  <div class="ride-item">
                    <div class="ride-row">
                      <div class="ride-info">
                        <strong><%= requestRow.getOrigin() %> -> <%= requestRow.getDestination() %></strong>
                        <small class="ride-meta">Passenger: <%= requestRow.getPassengerName() %></small>
                        <small class="ride-meta">Request ID: #<%= requestRow.getBookingId() %></small>
                        <small class="ride-meta">Requested seats: <%= requestRow.getRequestedSeats() %></small>
                        <small class="ride-meta">Departure: <%= formattedDeparture %></small>
                        <% if (!formattedDepartureTime.isBlank()) { %>
                          <small class="ride-meta">Departure time: <%= formattedDepartureTime %></small>
                        <% } %>
                        <small class="ride-meta">Requested at: <%= formattedRequestedAt %></small>
                        <small class="ride-meta">Status: <%= requestRow.getBookingStatus() %></small>
                      </div>
                      <% if ("pending".equalsIgnoreCase(requestRow.getBookingStatus())) { %>
                        <div class="ride-actions request-actions request-accept-stack">
                          <% if (driverVehicles.isEmpty()) { %>
                            <p class="ride-meta ride-meta--warn">Add a vehicle under My Vehicles to accept this request.</p>
                          <% } else { %>
                            <form method="post" action="<%= cp %>/dashboard/driver" class="dashboard-inline-form request-accept-form">
                              <input type="hidden" name="action" value="processPassengerRequest" />
                              <input type="hidden" name="bookingId" value="<%= requestRow.getBookingId() %>" />
                              <input type="hidden" name="decision" value="accept" />
                              <label class="request-vehicle-label">
                                <span class="request-vehicle-label-text">Vehicle for this ride</span>
                                <select name="vehicleId" class="login-input request-vehicle-select" required>
                                  <% for (Vehicle v : driverVehicles) { %>
                                    <option value="<%= v.getId() %>">
                                      <%= v.getColor() %> <%= v.getMake() %> <%= v.getModel() %> — plate <%= v.getPlate() %> (<%= v.getTotalSeats() %> seats)
                                    </option>
                                  <% } %>
                                </select>
                              </label>
                              <button type="submit" class="request-approve">Accept Request</button>
                            </form>
                          <% } %>
                        </div>
                      <% } %>
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