<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="model.Vehicle" %>
<%@ page import="model.User" %>
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
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Offer a Ride | UniRide</title>
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
                <a href="<%= cp %>/dashboard/driver" class="nav-btn-secondary">Back to Dashboard</a>
            </div>
        </nav>

        <div class="dashboard-main">
            <div class="dashboard-main-inner">
                <section class="dashboard-section">
                    <div class="dashboard-section-heading">
                        <h3>Offer a New Ride</h3>
                        <p>Fill out the details below to share your journey with other SJSU students.</p>
                    </div>

                    <form method="post" action="<%= cp %>/dashboard/driver" class="settings-form booking-form">
                        <input type="hidden" name="action" value="processCreateRide">

                        <div class="form-group">
                            <label><strong>Vehicle</strong></label>
                            <% if (vehicles.isEmpty()) { %>
                                <p class="ride-meta" style="color: #b45309;">Add a vehicle first before creating a ride.</p>
                            <% } else { %>
                                <select name="vehicleId" class="login-input" required>
                                    <option value="" disabled selected>Select a vehicle</option>
                                    <% for (Vehicle vehicle : vehicles) { %>
                                        <option value="<%= vehicle.getId() %>"><%= vehicle.getColor() %> <%= vehicle.getMake() %> <%= vehicle.getModel() %> (Plate <%= vehicle.getPlate() %>)</option>
                                    <% } %>
                                </select>
                            <% } %>
                        </div>

                        <div class="form-group">
                            <label><strong>Pickup Location (Origin)</strong></label>
                            <input type="text" name="origin" class="login-input" placeholder="e.g. King Library" required>
                        </div>

                        <div class="form-group">
                            <label><strong>Drop-off Location (Destination)</strong></label>
                            <input type="text" name="destination" class="login-input" placeholder="e.g. San Jose Diridon" required>
                        </div>

                        <div class="form-group">
                            <label><strong>Departure Date & Time</strong></label>
                            <input type="datetime-local" name="departureDate" class="login-input" required>
                        </div>

                        <div class="form-group">
                            <label><strong>Available Seats</strong></label>
                            <input type="number" name="seatsLeft" class="login-input" min="1" max="10" placeholder="Number of seats" required>
                        </div>

                        <hr class="rule">

                        <button type="submit" class="action-create u-w-100 u-text-center booking-submit" <%= vehicles.isEmpty() ? "disabled" : "" %>>
                            Post Ride Offer
                        </button>
                    </form>
                </section>
            </div>
        </div>
    </div>
</body>
</html>