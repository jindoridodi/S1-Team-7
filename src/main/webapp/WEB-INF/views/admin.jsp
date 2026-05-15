<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
String cp = request.getContextPath();
boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
java.util.List<String[]> pendingDrivers = (java.util.List<String[]>) request.getAttribute("pendingDrivers");
if (pendingDrivers == null) pendingDrivers = java.util.Collections.emptyList();
java.util.List<String[]> allRides = (java.util.List<String[]>) request.getAttribute("allRides");
if (allRides == null) allRides = java.util.Collections.emptyList();
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <title>Admin Panel | UniRide</title>
  <link rel="stylesheet" href="<%= cp %>/assets/css/common.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/home.css?v=20260515" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/login.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/dashboard.css?v=20260515" />
</head>
<body>
<div class="login-page dashboard-page">
  <nav class="navbar">
    <h1 class="logo"><a href="<%= cp %>/home">Uni<span class="highlight">Ride</span></a></h1>
    <div class="nav-links">
      <% if (isAdmin) { %>
        <form method="post" action="<%= cp %>/admin" class="dashboard-inline-form">
          <input type="hidden" name="action" value="adminLogout" />
          <button type="submit" class="nav-btn-secondary">Admin Logout</button>
        </form>
      <% } %>
    </div>
  </nav>

  <div class="dashboard-main">
    <div class="dashboard-main-inner">

      <% if (!isAdmin) { %>
        <div style="display:flex; justify-content:center; align-items:center; min-height:80vh; width:100%;">
          <div class="login-shell auth-shell">
            <header class="login-header">
              <h2>Admin Login</h2>
            </header>
            <form class="login-form" method="post" action="<%= cp %>/admin">
              <input type="hidden" name="action" value="adminLogin" />
              <% if (request.getAttribute("loginError") != null) { %>
                <p class="field-error"><%= request.getAttribute("loginError") %></p>
              <% } %>
              <label for="adminPassword">Admin Password</label>
              <input id="adminPassword" type="password" name="adminPassword" placeholder="Enter admin password" />
              <button type="submit" class="login-submit">Login</button>
            </form>
          </div>
        </div>

      <% } else { %>
        <section class="dashboard-section dashboard-passenger-section">
          <div class="dashboard-section-heading">
            <h3>Pending Driver Verifications</h3>
            <p>Review and approve or reject driver applications below.</p>
          </div>

          <% if (pendingDrivers.isEmpty()) { %>
            <div class="dashboard-empty">
              <p>No pending driver applications.</p>
            </div>
          <% } else { %>
            <div class="ride-list">
              <% for (String[] driver : pendingDrivers) { %>
                <div class="ride-item">
                  <div class="ride-row">
                    <div class="ride-info">
                      <strong><%= driver[1] %> <%= driver[2] %></strong>
                      <small class="ride-meta">Email: <%= driver[3] %></small>
                      <small class="ride-meta">SJSU ID: <%= driver[4] %></small>
                      <small class="ride-meta">License: <%= driver[5] %></small>
                    </div>
                    <div class="ride-actions">
                      <form method="post" action="<%= cp %>/admin" class="dashboard-inline-form">
                        <input type="hidden" name="action" value="verifyDriver" />
                        <input type="hidden" name="userId" value="<%= driver[0] %>" />
                        <button type="submit" class="request-approve">Approve</button>
                      </form>
                      <form method="post" action="<%= cp %>/admin" class="dashboard-inline-form">
                        <input type="hidden" name="action" value="rejectDriver" />
                        <input type="hidden" name="userId" value="<%= driver[0] %>" />
                        <button type="submit" class="request-decline">Reject</button>
                      </form>
                    </div>
                  </div>
                </div>
              <% } %>
            </div>
          <% } %>
        </section>

        <section class="dashboard-section dashboard-passenger-section admin-rides-section">
          <div class="dashboard-section-heading">
            <h3>All ride history</h3>
            <p>Every ride in the system. Cancel rides that are still active (open or full, or in progress) before they are completed.</p>
          </div>
          <% if (allRides.isEmpty()) { %>
            <div class="dashboard-empty">
              <p>No rides recorded yet.</p>
            </div>
          <% } else { %>
            <div class="admin-rides-toolbar">
              <label class="admin-rides-search-label" for="adminRideSearch">Filter by location or details</label>
              <input id="adminRideSearch" class="admin-rides-search" type="search" placeholder="Search pickup, destination, driver, email, status&hellip;" autocomplete="off" />
            </div>
            <p id="adminRideFilterEmpty" class="admin-rides-filter-empty is-hidden" role="status">No rides match your search.</p>
            <div class="ride-list admin-rides-list" id="adminRidesList">
              <% for (String[] rideRow : allRides) {
                   String rideStatus = rideRow.length > 8 ? rideRow[8] : "";
                   boolean canCancel = rideStatus != null && !rideStatus.equalsIgnoreCase("cancelled") && !rideStatus.equalsIgnoreCase("completed");
                   String origin = rideRow.length > 4 && rideRow[4] != null ? rideRow[4] : "";
                   String destination = rideRow.length > 5 && rideRow[5] != null ? rideRow[5] : "";
              %>
                <div class="ride-item js-admin-ride-item">
                  <span class="is-hidden admin-ride-search-source" aria-hidden="true"><%= rideRow[0] %> <%= rideRow[1] %> <%= rideRow[2] %> <%= rideRow[3] %> <%= origin %> <%= destination %> <%= rideRow[6] %> <%= rideRow[7] %> <%= rideStatus %></span>
                  <div class="ride-row">
                    <div class="ride-info">
                      <strong class="admin-ride-route-title"><%= origin %> &rarr; <%= destination %></strong>
                      <small class="ride-meta admin-ride-id-meta">Ride ID <%= rideRow[0] %> &middot; <span class="ride-meta-inline-status"><%= rideStatus != null ? rideStatus : "" %></span></small>
                      <small class="ride-meta">Driver: <%= rideRow[1] %> <%= rideRow[2] %> (<%= rideRow[3] %>)</small>
                      <small class="ride-meta">Departs: <%= rideRow[6] %> &middot; Seats left: <%= rideRow[7] %></small>
                    </div>
                    <div class="ride-actions">
                      <% if (canCancel) { %>
                        <form method="post" action="<%= cp %>/admin" class="dashboard-inline-form" onsubmit="return confirm('Cancel this ride for all passengers?');">
                          <input type="hidden" name="action" value="cancelRide" />
                          <input type="hidden" name="rideId" value="<%= rideRow[0] %>" />
                          <button type="submit" class="request-decline">Cancel ride</button>
                        </form>
                      <% } else { %>
                        <span class="ride-meta">&mdash;</span>
                      <% } %>
                    </div>
                  </div>
                </div>
              <% } %>
            </div>
          <% } %>
        </section>

      <% } %>

    </div>
  </div>
</div>
<% if (isAdmin && allRides != null && !allRides.isEmpty()) { %>
<script>
(function () {
  var input = document.getElementById("adminRideSearch");
  var items = document.querySelectorAll(".js-admin-ride-item");
  var emptyMsg = document.getElementById("adminRideFilterEmpty");
  if (!input || !items.length) return;

  function filterRides() {
    var q = (input.value || "").trim().toLowerCase();
    var visible = 0;
    for (var i = 0; i < items.length; i++) {
      var el = items[i];
      var blobEl = el.querySelector(".admin-ride-search-source");
      var hay = blobEl ? blobEl.textContent.toLowerCase() : "";
      var show = !q || hay.indexOf(q) !== -1;
      el.style.display = show ? "" : "none";
      if (show) visible++;
    }
    if (emptyMsg) {
      emptyMsg.classList.toggle("is-hidden", visible !== 0 || items.length === 0);
    }
  }

  input.addEventListener("input", filterRides);
  input.addEventListener("search", filterRides);
})();
</script>
<% } %>
</body>
</html>