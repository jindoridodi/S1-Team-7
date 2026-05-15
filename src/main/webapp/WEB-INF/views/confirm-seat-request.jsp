<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User, model.Ride, java.time.LocalDateTime, java.time.format.DateTimeFormatter" %>
<%
String cp = request.getContextPath();
User currentUser = (User) session.getAttribute("currentUser");
Ride ride = (Ride) request.getAttribute("ride");
if (ride == null) {
  response.sendRedirect(cp + "/dashboard/passenger?error=rideUnavailable");
  return;
}
String formattedDeparture = ride.getDepartureDate();
String formattedTime = "";
try {
  DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  LocalDateTime dt = LocalDateTime.parse(ride.getDepartureDate(), parser);
  formattedDeparture = dt.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
  formattedTime = dt.format(DateTimeFormatter.ofPattern("h:mm a"));
} catch (Exception ignored) {
}
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Request a Seat | UniRide</title>
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
        <a href="<%= cp %>/dashboard/passenger?action=searchRides" class="nav-btn-secondary">Back to search</a>
      </div>
    </nav>

    <div class="dashboard-main">
      <div class="dashboard-main-inner">
        <section class="dashboard-section dashboard-passenger-section">
          <div class="dashboard-section-heading">
            <h3>Request a seat</h3>
            <p>Review this ride, then submit your request. Searching does not book a seat until you confirm here.</p>
          </div>

          <div class="ride-item">
            <div class="ride-info">
              <strong><%= ride.getOrigin() %> &rarr; <%= ride.getDestination() %></strong>
              <small class="ride-meta">Driver: <%= ride.getDriverName() != null ? ride.getDriverName() : "—" %></small>
              <% if (ride.getDriverGender() != null && !ride.getDriverGender().isBlank()) { %>
                <small class="ride-meta">Driver gender: <%= ride.getDriverGender() %></small>
              <% } %>
              <small class="ride-meta">Vehicle: <%= ride.getVehicleInfo() != null ? ride.getVehicleInfo() : "—" %></small>
              <small class="ride-meta">Departure: <%= formattedDeparture %><% if (!formattedTime.isBlank()) { %> at <%= formattedTime %><% } %></small>
              <% if (ride.getSeatsLeft() > 0) { %>
                <small class="ride-meta ride-meta--success">Seats available: <%= ride.getSeatsLeft() %></small>
              <% } else { %>
                <small class="ride-meta ride-meta--warn">Listed seats: <%= ride.getSeatsLeft() %> — your request will still be sent to the driver</small>
              <% } %>
            </div>
          </div>

          <form method="post" action="<%= cp %>/dashboard/passenger" class="settings-form booking-form u-mt-lg">
            <input type="hidden" name="action" value="requestSeatOnRide" />
            <input type="hidden" name="rideId" value="<%= ride.getId() %>" />

            <div class="form-group">
              <label for="seatsRequested"><strong>Seats requested</strong></label>
              <input type="number" id="seatsRequested" name="seatsRequested" class="login-input"
                     min="1" max="10" value="1" required />
            </div>

            <button type="submit" class="action-create u-w-100 u-text-center booking-submit">Submit seat request</button>
            <a href="<%= cp %>/dashboard/passenger?action=searchRides" class="nav-btn-secondary u-mt-md" style="display:inline-block;text-align:center;">Cancel</a>
          </form>
        </section>
      </div>
    </div>
  </div>
</body>
</html>
