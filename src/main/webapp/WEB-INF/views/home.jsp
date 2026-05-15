<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%
String cp = request.getContextPath();
User currentUser = (User) session.getAttribute("currentUser");
String dashboardPath = cp + "/login";
if (currentUser != null) {
  dashboardPath = cp + "/dashboard/passenger";
  if (currentUser.hasRole("driver")) {
    dashboardPath = cp + "/dashboard/driver";
  }
}
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>UniRide</title>
  <link rel="stylesheet" href="<%= cp %>/assets/css/common.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/home.css?v=20260427" />
</head>
<body>
  <div class="home-container">
    <nav class="navbar">
      <h1 class="logo">Uni<span class="highlight">Ride</span></h1>
      <div class="nav-links">
        <a class="nav-btn-secondary" href="<%= dashboardPath %>">Dashboard</a>
        <a class="nav-btn-primary" href="<%= cp %>/signup">Sign Up</a>
      </div>
    </nav>

    <main class="hero-section">
      <div class="hero-wrapper">
        <div class="hero-content">
          <div class="campus-tag">SJSU Campus Network</div>
          <h1>Commute smarter to <span class="highlight">SJSU</span></h1>
          <p>
            The safe, reliable way for Spartans to share rides, save money, and
            reduce campus traffic - all in one place.
          </p>
          <div class="hero-btns">
            <a class="main-btn-primary" href="<%= cp %>/signup">Get Started</a>
            <a class="main-btn-outline" href="<%= cp %>/login">View Rides -&gt;</a>
          </div>
        </div>

        <div class="search-card">
          <h4 class="card-label">Find a Ride Now</h4>
          <div class="input-group">
            <label>FROM</label>
            <input type="text" id="search-origin" placeholder="Your pickup location" />
          </div>
          <div class="input-group">
            <label>TO</label>
            <input type="text" id="search-destination" placeholder="San Jose State University" />
          </div>
          <div class="input-row">
            <div class="input-group">
              <label>DATE</label>
              <input type="date" id="search-date" />
            </div>
            <div class="input-group input-group--seats">
              <label>SEATS</label>
              <select id="search-seats">
                <option>1</option>
                <option>2</option>
                <option>3</option>
              </select>
            </div>
          </div>
          <button class="search-submit" type="button" onclick="searchRides()">Search rides &rarr;</button>
        </div>
      </div>
    </main>

    <section class="stats-bar">
      <div class="stat-item"><h3>2k+</h3><p>SPARTAN RIDERS</p></div>
      <div class="stat-item"><h3>320</h3><p>RIDES THIS WEEK</p></div>
      <div class="stat-item"><h3>4.9</h3><p>AVG RATING</p></div>
      <div class="stat-item"><h3 class="highlight">$18</h3><p>AVG TRIP SAVED</p></div>
    </section>

    <section class="features-section">
      <p class="section-subtitle">Why UniRide</p>
      <h2 class="section-title">Built for Spartans,<br/>by Spartans</h2>

      <div class="features-grid">
        <div class="feature-card">
          <div class="icon-box">Protect</div>
          <h3>Verified students only</h3>
          <p>Only users with @sjsu.edu emails can join - so every rider and driver is a trusted Spartan.</p>
        </div>
        <div class="feature-card">
          <div class="icon-box">Save</div>
          <h3>Split the costs</h3>
          <p>Share gas and parking costs with students heading the same way. Save up to $20 per trip.</p>
        </div>
        <div class="feature-card">
          <div class="icon-box">Green</div>
          <h3>Greener campus</h3>
          <p>Fewer solo cars means less congestion at North and South garages.</p>
        </div>
      </div>
    </section>

    <footer class="home-footer">
      <p>Copyright 2026 UniRide | Built for Spartans</p>
      <div class="footer-links">
        <span>Privacy</span>
        <span>Terms</span>
        <span>Contact</span>
      </div>
    </footer>
  </div>
  <script>
    function searchRides() {
      var origin      = document.getElementById('search-origin').value.trim();
      var destination = document.getElementById('search-destination').value.trim();
      var date        = document.getElementById('search-date').value;
      var url = '<%= cp %>/dashboard/passenger?action=searchRides'
              + '&searchOrigin=' + encodeURIComponent(origin)
              + '&searchDestination=' + encodeURIComponent(destination)
              + '&searchDate=' + encodeURIComponent(date);
      <% if (currentUser == null) { %>
      window.location.href = '<%= cp %>/login?redirect=' + encodeURIComponent(url);
      <% } else { %>
      window.location.href = url;
      <% } %>
    }
  </script>
</body>
</html>
