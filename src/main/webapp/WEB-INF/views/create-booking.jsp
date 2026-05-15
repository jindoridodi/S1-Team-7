<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%
String cp = request.getContextPath();
User currentUser = (User) session.getAttribute("currentUser");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Post Open Ride Request | UniRide</title>
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
                <a href="<%= cp %>/dashboard/passenger?action=searchRides" class="nav-btn-secondary">Search rides</a>
                <a href="<%= cp %>/dashboard/passenger" class="nav-btn-secondary">Dashboard</a>
            </div>
        </nav>

        <div class="dashboard-main">
            <div class="dashboard-main-inner">
                <section class="dashboard-section">
                    <div class="dashboard-section-heading">
                        <h3>Post an open ride request</h3>
                        <p>
                            Use this when no listed ride matches your trip. Drivers can see your request and offer a ride.
                            This is not the same as searching or booking a seat on an existing listing.
                        </p>
                    </div>

                    <form method="post" action="<%= cp %>/dashboard/passenger" class="settings-form booking-form">
                        <input type="hidden" name="action" value="postOpenRideRequest">

                        <div class="form-group">
                            <label><strong>Pickup location (origin)</strong></label>
                            <input type="text" name="origin" class="login-input" placeholder="e.g. King Library" required>
                        </div>

                        <div class="form-group">
                            <label><strong>Drop-off location (destination)</strong></label>
                            <input type="text" name="destination" class="login-input" placeholder="e.g. San Jose Diridon" required>
                        </div>

                        <div class="form-group">
                            <label><strong>Preferred departure date &amp; time</strong></label>
                            <input type="datetime-local" name="departureDate" class="login-input" required>
                        </div>

                        <div class="form-group">
                            <label><strong>Seats needed</strong></label>
                            <input type="number" name="seatsLeft" class="login-input" min="1" max="10" placeholder="Number of seats" required>
                        </div>

                        <hr class="rule">

                        <button type="submit" class="action-create u-w-100 u-text-center booking-submit">
                            Post request for drivers
                        </button>
                    </form>
                </section>
            </div>
        </div>
    </div>
</body>
</html>
