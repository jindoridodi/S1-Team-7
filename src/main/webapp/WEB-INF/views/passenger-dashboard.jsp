<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User, java.time.LocalDateTime, java.time.format.DateTimeFormatter" %>
<%
String cp = request.getContextPath();
User currentUser = (User) session.getAttribute("currentUser");
java.util.List<model.UpcomingRide> upcomingRides = (java.util.List<model.UpcomingRide>) request.getAttribute("upcomingRides");
if (upcomingRides == null) {
  upcomingRides = java.util.Collections.emptyList();
}
String successMessage = (String) request.getAttribute("successMessage");
String errorMessage = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Passenger Dashboard | UniRide</title>
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
        <a href="<%= cp %>/settings" class="nav-btn-primary dashboard-settings">Settings</a>
        <form method="post" action="<%= cp %>/logout" class="dashboard-inline-form">
          <button type="submit" class="nav-btn-secondary dashboard-signout">Sign Out</button>
        </form>
      </div>
    </nav>

    <div class="dashboard-main">
      <div class="dashboard-main-inner">

        <%
          java.util.List<String[]> notifications = (java.util.List<String[]>) request.getAttribute("notifications");
          if (notifications == null) notifications = java.util.Collections.emptyList();
          long unreadCount = notifications.stream().filter(n -> "unread".equals(n[3])).count();
          boolean hasFlashMessage = successMessage != null || errorMessage != null;
          boolean showNotificationsSection = !notifications.isEmpty() || hasFlashMessage;
          if (hasFlashMessage) {
            unreadCount += (successMessage != null ? 1 : 0) + (errorMessage != null ? 1 : 0);
          }
        %>

        <% if (showNotificationsSection) { %>
          <section class="dashboard-section dashboard-passenger-section">
            <div class="dashboard-section-heading">
              <h3>Notifications <% if (unreadCount > 0) { %><span class="notif-badge"><%= unreadCount %></span><% } %></h3>
            </div>
            <div class="ride-list">
              <% if (successMessage != null) { %>
                <div class="ride-item notif-unread">
                  <div class="ride-row">
                    <div class="ride-info">
                      <small class="ride-meta"><%= successMessage %></small>
                      <small class="ride-meta">Just now</small>
                    </div>
                  </div>
                </div>
              <% } %>
              <% if (errorMessage != null) { %>
                <div class="ride-item notif-error">
                  <div class="ride-row">
                    <div class="ride-info">
                      <small class="ride-meta"><%= errorMessage %></small>
                      <small class="ride-meta">Just now</small>
                    </div>
                  </div>
                </div>
              <% } %>
              <% for (String[] notif : notifications) {
                   boolean isUnread = "unread".equals(notif[3]);
              %>
                <div class="ride-item <%= isUnread ? "notif-unread" : "" %>">
                  <div class="ride-row">
                    <div class="ride-info">
                      <small class="ride-meta"><%= notif[1] %></small>
                      <small class="ride-meta"><%= notif[2] %></small>
                    </div>
                    <% if (isUnread) { %>
                      <form method="post" action="<%= cp %>/dashboard/passenger" class="dashboard-inline-form">
                        <input type="hidden" name="action" value="markNotifRead" />
                        <input type="hidden" name="notifId" value="<%= notif[0] %>" />
                        <button type="submit" class="request-approve">Mark Read</button>
                      </form>
                    <% } %>
                  </div>
                </div>
              <% } %>
            </div>
          </section>
        <% } %>



        <section class="dashboard-hero dashboard-section">
          <div class="dashboard-hero-copy">
            <span class="campus-tag">Passenger Hub</span>
            <h2>Find the right ride and keep your trips organized.</h2>
            <p>
              Search for open rides first, then request a seat only on the trip you choose.
              Posting an open request is separate if no driver has listed a match yet.
            </p>
          </div>

                    <div class="dashboard-actions">
            <a class="login-submit action-find u-inline-flex-center" href="<%= cp %>/dashboard/passenger?action=searchRides">Search rides</a>
            <a class="login-submit action-bookings u-inline-flex-center" href="<%= cp %>/dashboard/passenger?action=showRequestRideForm">Request a Ride</a>
            <a class="login-submit action-bookings u-inline-flex-center" href="<%= cp %>/dashboard/passenger?action=showRideHistory">Ride History</a>
            <a class="login-submit action-bookings u-inline-flex-center" href="<%= cp %>/dashboard/passenger?action=showSavedRoutes">My Saved Routes</a>
          </div>
        </section>


        <section class="dashboard-section dashboard-passenger-section">
          <div class="dashboard-section-heading">
            <h3>Upcoming Rides</h3>
            <p>Your pending or accepted rides are shown here. You can cancel before departure.</p>
          </div>

          <% if (upcomingRides.isEmpty()) { %>
            <div class="dashboard-empty dashboard-empty--spaced">
              <p>No upcoming rides yet.</p>
            </div>
          <% } else { %>
            <div class="ride-list ride-list--spaced">
              <% for (model.UpcomingRide upcomingRide : upcomingRides) {
                   String rawDeparture = upcomingRide.getDepartureDate();
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
                      <strong><%= upcomingRide.getOrigin() %> → <%= upcomingRide.getDestination() %></strong><br />
                      <small class="ride-meta">Booking ID: #<%= upcomingRide.getBookingId() %></small>
                      <small class="ride-meta">Status: <%= upcomingRide.getBookingStatus() %></small>
                      <small class="ride-meta">Driver: <%= upcomingRide.getDriverName() != null ? upcomingRide.getDriverName() : "Not assigned" %></small>
                      <% if (upcomingRide.getDriverGender() != null && !upcomingRide.getDriverGender().isBlank()) { %>
                        <small class="ride-meta">Driver gender: <%= upcomingRide.getDriverGender() %></small>
                      <% } %>
                      <small class="ride-meta">Vehicle: <%= upcomingRide.getVehicleInfo() != null ? upcomingRide.getVehicleInfo() : "Not assigned" %></small>
                      <small class="ride-meta">Departure: <%= formattedDeparture %></small>
                      <% if (!formattedTime.isBlank()) { %>
                        <small class="ride-meta">Time: <%= formattedTime %></small>
                      <% } %>
                      <small class="ride-meta">Seats: <%= upcomingRide.getSeats() %></small>
                    </div>
                    <div class="ride-actions request-actions">
                      <form method="post" action="<%= cp %>/dashboard/passenger" class="dashboard-inline-form">
                        <input type="hidden" name="action" value="cancelUpcomingRide" />
                        <input type="hidden" name="bookingId" value="<%= upcomingRide.getBookingId() %>" />
                        <button type="submit" class="request-decline">Cancel Ride</button>
                      </form>
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