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
        <%-- Admin login form --%>
        <div class="login-shell" style="max-width:400px; margin: 4rem auto;">
          <h2 style="margin-bottom:1.5rem;">Admin Login</h2>
          <% if (request.getAttribute("loginError") != null) { %>
            <p class="field-error"><%= request.getAttribute("loginError") %></p>
          <% } %>
          <form method="post" action="<%= cp %>/admin">
            <input type="hidden" name="action" value="adminLogin" />
            <label>Admin Password</label>
            <input type="password" name="adminPassword" placeholder="Enter admin password" style="width:100%; margin-bottom:1rem;" />
            <button type="submit" class="login-submit" style="width:100%;">Login</button>
          </form>
        </div>

      <% } else { %>
        <%-- Admin dashboard --%>
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