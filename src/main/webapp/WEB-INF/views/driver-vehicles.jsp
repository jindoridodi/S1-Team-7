<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="model.User" %>
<%@ page import="model.Vehicle" %>
<%
String cp = request.getContextPath();
User currentUser = (User) session.getAttribute("currentUser");
List<Vehicle> vehicles = (List<Vehicle>) request.getAttribute("vehicles");
if (vehicles == null) {
  vehicles = java.util.Collections.emptyList();
}
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>My Vehicles | UniRide</title>
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
        <button type="button" class="nav-btn-secondary dashboard-role-switch-ui" aria-disabled="true" tabindex="-1">Switch to passenger</button>
        <a href="<%= cp %>/settings" class="nav-btn-primary dashboard-settings">Settings</a>
        <a href="<%= cp %>/dashboard/driver" class="nav-btn-secondary">Back to Dashboard</a>
        <form method="post" action="<%= cp %>/logout" class="dashboard-inline-form">
          <button type="submit" class="nav-btn-secondary dashboard-signout">Sign Out</button>
        </form>
      </div>
    </nav>

    <div class="dashboard-main">
      <div class="dashboard-main-inner">
        <section class="dashboard-section dashboard-vehicle-card">
          <div class="dashboard-section-heading">
            <h3>My Registered Vehicles</h3>
            <p>Add each vehicle once and keep its details current for matching and pickup.</p>
          </div>

          <% if (request.getAttribute("error") != null) { %>
            <p style="color:red; text-align:center; margin-top:10px;">
              <%= request.getAttribute("error") %>
            </p>
          <% } %>

          <div class="vehicle-add">
            <button
              type="button"
              class="vehicle-add-toggle"
              aria-expanded="false"
              aria-controls="vehicle-add-panel"
              onclick="toggleAddVehicle()"
            >
              <span class="vehicle-add-title">Add a New Vehicle</span>
              <span class="vehicle-add-icon" aria-hidden="true">+</span>
            </button>

            <div id="vehicle-add-panel" class="vehicle-add-panel is-hidden">
              <form method="post" action="<%= cp %>/dashboard/driver" class="dashboard-form-grid">
                <input type="hidden" name="action" value="addVehicle" />
                <input type="text" name="make" class="login-input" placeholder="Vehicle make (e.g. Toyota)" required />
                <input type="text" name="model" class="login-input" placeholder="Vehicle model (e.g. Camry)" required />
                <input type="text" name="color" class="login-input" placeholder="Vehicle color" required />
                <input type="text" name="plate" class="login-input" placeholder="License plate" required />
                <input type="number" name="totalSeats" class="login-input" placeholder="Total seats" min="1" required />
                <button class="login-submit dashboard-add-vehicle u-w-100 u-mt-lg" type="submit">Add Vehicle</button>
              </form>
            </div>
          </div>

          <div class="vehicle-list-container">
            <% if (vehicles.isEmpty()) { %>
              <p class="dashboard-empty">No vehicles added yet.</p>
            <% } else { %>
              <% for (Vehicle vehicle : vehicles) { %>
                <div class="vehicle-item-wrapper" id="vehicle-wrapper-<%= vehicle.getId() %>">
                  <div class="vehicle-row" id="vehicle-row-<%= vehicle.getId() %>">
                    <div class="vehicle-info">
                      <strong><%= vehicle.getColor() %> <%= vehicle.getMake() %> <%= vehicle.getModel() %></strong><br />
                      <small class="vehicle-meta">Plate: <%= vehicle.getPlate() %></small>
                      <small class="vehicle-meta">Total seats: <%= vehicle.getTotalSeats() %></small>
                    </div>
                    <div class="vehicle-actions">
                      <button type="button" class="vehicle-edit" onclick="toggleEditForm('<%= vehicle.getId() %>')">Edit</button>
                      <form method="post" action="<%= cp %>/dashboard/driver" class="dashboard-inline-form">
                        <input type="hidden" name="action" value="deleteVehicle" />
                        <input type="hidden" name="vehicleId" value="<%= vehicle.getId() %>" />
                        <button type="submit" class="vehicle-delete">Delete</button>
                      </form>
                    </div>
                  </div>

                  <div class="vehicle-edit-state is-hidden" id="edit-form-<%= vehicle.getId() %>">
                    <form method="post" action="<%= cp %>/dashboard/driver" class="vehicle-edit-form">
                      <input type="hidden" name="action" value="updateVehicle" />
                      <input type="hidden" name="vehicleId" value="<%= vehicle.getId() %>" />
                      <div class="edit-form-inputs">
                        <input type="text" name="make" class="login-input" value="<%= vehicle.getMake() %>" placeholder="Vehicle make" required />
                        <input type="text" name="model" class="login-input" value="<%= vehicle.getModel() %>" placeholder="Vehicle model" required />
                        <input type="text" name="color" class="login-input" value="<%= vehicle.getColor() %>" placeholder="Vehicle color" required />
                        <input type="text" name="plate" class="login-input" value="<%= vehicle.getPlate() %>" placeholder="License plate" required />
                        <input type="number" name="totalSeats" class="login-input" value="<%= vehicle.getTotalSeats() %>" placeholder="Total seats" min="1" required />
                      </div>
                      <div class="form-button-group">
                        <button type="submit" class="login-submit dashboard-save-vehicle">Save Changes</button>
                        <button type="button" class="vehicle-cancel" onclick="toggleEditForm('<%= vehicle.getId() %>')">Cancel</button>
                      </div>
                    </form>
                  </div>
                </div>
              <% } %>
            <% } %>
          </div>
        </section>
      </div>
    </div>
  </div>

  <script>
    function toggleAddVehicle() {
      const toggle = document.querySelector('.vehicle-add-toggle');
      const panel = document.getElementById('vehicle-add-panel');
      if (!toggle || !panel) return;

      const isExpanded = toggle.getAttribute('aria-expanded') === 'true';
      toggle.setAttribute('aria-expanded', String(!isExpanded));
      panel.classList.toggle('is-hidden', isExpanded);
      const icon = toggle.querySelector('.vehicle-add-icon');
      if (icon) icon.textContent = isExpanded ? '+' : '–';
    }

    function toggleEditForm(vehicleId) {
      const form = document.getElementById('edit-form-' + vehicleId);
      const row = document.getElementById('vehicle-row-' + vehicleId);
      form.classList.toggle('is-hidden');
      row.classList.toggle('is-hidden');
    }
  </script>
</body>
</html>
