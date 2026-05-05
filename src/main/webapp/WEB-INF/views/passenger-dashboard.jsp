<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User, java.time.LocalDateTime, java.time.format.DateTimeFormatter" %>
<%
String cp = request.getContextPath();
User currentUser = (User) session.getAttribute("currentUser");
boolean canSwapDashboard = currentUser != null && currentUser.hasRole("driver") && currentUser.hasRole("passenger");
java.util.List<model.UpcomingRide> upcomingRides = (java.util.List<model.UpcomingRide>) request.getAttribute("upcomingRides");
if (upcomingRides == null) {
  upcomingRides = java.util.Collections.emptyList();
}
java.util.List<String[]> savedRoutes = (java.util.List<String[]>) request.getAttribute("savedRoutes");
if (savedRoutes == null) savedRoutes = java.util.Collections.emptyList();
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
        <% if (canSwapDashboard) { %>
          <a href="<%= cp %>/dashboard/driver" class="nav-btn-secondary">Switch to Driver</a>
        <% } %>
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
        %>

        <% if (!notifications.isEmpty()) { %>
          <section class="dashboard-section dashboard-passenger-section">
            <div class="dashboard-section-heading">
              <h3>Notifications <% if (unreadCount > 0) { %><span class="notif-badge"><%= unreadCount %></span><% } %></h3>
            </div>
            <div class="ride-list">
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

        <section class="dashboard-section dashboard-passenger-section">
          <div class="dashboard-section-heading">
            <h3>My Saved Routes</h3>
            <p>Save your frequent routes and we'll highlight matching rides for you.</p>
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

          <form method="post" action="<%= cp %>/dashboard/passenger" class="dashboard-form-grid">
            <input type="hidden" name="action" value="saveRoute" />
            <input type="text" name="startLocation" placeholder="From (e.g. Fremont)" required />
            <input type="text" name="endLocation" placeholder="To (e.g. SJSU)" required />
            <button type="submit" class="login-submit">Save Route</button>
          </form>
        </section>

        <section class="dashboard-hero dashboard-section">
          <div class="dashboard-hero-copy">
            <span class="campus-tag">Passenger Hub</span>
            <h2>Find the right ride and keep your trips organized.</h2>
            <p>
              Search for rides near campus, review your bookings, and manage your passenger account from one place.
            </p>
          </div>

          <div class="dashboard-actions">
            <a class="login-submit action-find u-inline-flex-center" href="<%= cp %>/dashboard/passenger?action=showCreateBookingForm">Find a Ride</a>
            <a class="login-submit action-bookings u-inline-flex-center" href="<%= cp %>/dashboard/passenger?action=showRideHistory">Ride History</a>
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

        <section class="dashboard-section dashboard-passenger-section">
          <div class="dashboard-section-heading">
            <h3>Available Rides</h3>
            <p>Search results will appear here once rides are available for your route.</p>
          </div>

          <div class="dashboard-search-filters">
            <input id="rides-filter-text" type="text" placeholder="Filter by origin, destination, driver, or vehicle" class="dashboard-search dashboard-search--text" />
            <input id="rides-filter-date" type="date" class="dashboard-search dashboard-search--date" />
            <input id="rides-filter-hours" type="number" placeholder="Hours until departure" class="dashboard-search" min="0" />
          </div>

          <div id="rides-list" class="ride-list ride-list--spaced">
            <%
              java.util.List<model.Ride> availableRides = (java.util.List<model.Ride>) request.getAttribute("availableRides");
              if (availableRides == null) {
                availableRides = java.util.Collections.emptyList();
              }
            %>

            <% if (availableRides.isEmpty()) { %>
              <div id="no-rides" class="dashboard-empty dashboard-empty--spaced">
                <p>No active rides found for your search.</p>
              </div>
            <% } else { %>
              <div id="no-rides" class="dashboard-empty dashboard-empty--spaced is-hidden">
                <p>No active rides found for your search.</p>
              </div>
              <% for (model.Ride ride : availableRides) {
                   boolean isMatch = false;
                   for (String[] sr : savedRoutes) {
                     if (ride.getOrigin().toLowerCase().contains(sr[1].toLowerCase()) &&
                         ride.getDestination().toLowerCase().contains(sr[2].toLowerCase())) {
                       isMatch = true;
                       break;
                     }
                   }
              %>
                <div class="ride-item <%= isMatch ? "ride-item--highlight" : "" %>" data-origin="<%= ride.getOrigin().toLowerCase() %>" data-destination="<%= ride.getDestination().toLowerCase() %>" data-driver="<%= (ride.getDriverName() == null ? "" : ride.getDriverName()).toLowerCase() %>" data-vehicle="<%= (ride.getVehicleInfo() == null ? "" : ride.getVehicleInfo()).toLowerCase() %>" data-departure="<%= ride.getDepartureDate().toLowerCase() %>">
                  <div class="ride-row">
                    <div class="ride-info">
                      <strong><%= ride.getOrigin() %> → <%= ride.getDestination() %></strong><br />
                      <% if (isMatch) { %><small class="ride-meta" style="color:#f5a623;">★ Matches your saved route</small><% } %>
                      <small class="ride-meta">Status: <%= ride.getStatus() %></small>
                      <%
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
                      <small class="ride-meta">Driver: <%= ride.getDriverName() != null ? ride.getDriverName() : "Not assigned" %></small>
                      <small class="ride-meta">Vehicle: <%= ride.getVehicleInfo() != null ? ride.getVehicleInfo() : "Not assigned" %></small>
                      <small class="ride-meta">Departure: <%= formattedDeparture %></small>
                      <% if (!formattedTime.isBlank()) { %>
                        <small class="ride-meta">Time: <%= formattedTime %></small>
                      <% } %>
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
                        <form method="post" action="<%= cp %>/dashboard/passenger" class="dashboard-inline-form ride-book-form">
                          <input type="hidden" name="action" value="bookExistingRide" />
                          <input type="hidden" name="rideId" value="<%= ride.getId() %>" />
                          <input type="number"
                                 name="seatsRequested"
                                 class="ride-book-seats"
                                 min="1"
                                 max="<%= ride.getSeatsLeft() %>"
                                 value="1"
                                 required />
                          <button type="submit" class="request-approve">Book</button>
                        </form>
                      <% } else { %>
                        <span class="ride-meta">Not available.</span>
                      <% } %>
                    </div>
                  </div>
                </div>
              <% } %>
            <% } %>
          </div>

          <script>
            const params = new URLSearchParams(window.location.search);
            if (params.get('searchDestination')) document.getElementById('rides-filter-text').value = params.get('searchDestination');
            else if (params.get('searchOrigin')) document.getElementById('rides-filter-text').value = params.get('searchOrigin');
            if (params.get('searchDate')) document.getElementById('rides-filter-date').value = params.get('searchDate');

            (function() {
              const textInput = document.getElementById('rides-filter-text');
              const dateInput = document.getElementById('rides-filter-date');
              const hoursInput = document.getElementById('rides-filter-hours');
              const list = document.getElementById('rides-list');
              const noRides = document.getElementById('no-rides');

              function filter() {
                const hoursQuery = hoursInput.value.trim();
                const textQuery = textInput.value.trim().toLowerCase();
                const dateQuery = dateInput.value.trim();
                const items = list.querySelectorAll('.ride-item');
                let visible = 0;
                items.forEach(item => {
                  const origin = item.dataset.origin || '';
                  const destination = item.dataset.destination || '';
                  const driver = item.dataset.driver || '';
                  const vehicle = item.dataset.vehicle || '';
                  const departure = item.dataset.departure || '';
                  const departureDate = departure.split(' ')[0] || '';
                  let hoursMatches = true;

                  if (hoursQuery) {
                    const now = new Date();
                    const rideTime = new Date(departure.replace(' ', 'T'));
                    const diffHours = (rideTime - now) / (1000 * 60 * 60);
                    hoursMatches = diffHours >= 0 && diffHours <= parseFloat(hoursQuery);
                  }
                  const textMatches = !textQuery || origin.includes(textQuery) || destination.includes(textQuery) || driver.includes(textQuery) || vehicle.includes(textQuery);
                  const dateMatches = !dateQuery || departureDate === dateQuery;
                  if (textMatches && dateMatches && hoursMatches) {
                    item.style.display = '';
                    visible++;
                  } else {
                    item.style.display = 'none';
                  }
                });
                noRides.style.display = visible === 0 ? '' : 'none';
              }

              textInput.addEventListener('input', filter);
              dateInput.addEventListener('input', filter);
              hoursInput.addEventListener('input', filter);
            })();
          </script>

        </section>
      </div>
    </div>
  </div>
</body>
</html>