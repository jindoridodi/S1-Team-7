<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
String cp = request.getContextPath();
boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
java.util.List<String[]> pendingDrivers = (java.util.List<String[]>) request.getAttribute("pendingDrivers");
if (pendingDrivers == null) pendingDrivers = java.util.Collections.emptyList();
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <title>Admin Panel | UniRide</title>
  <link rel="stylesheet" href="<%= cp %>/assets/css/common.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/home.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/login.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/dashboard.css?v=20260427" />
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
      <% } %>

    </div>
  </div>
</div>
</body>
</html>