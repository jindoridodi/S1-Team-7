<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%
String cp = request.getContextPath();
User currentUser = (User) session.getAttribute("currentUser");
Object dashboardAttr = request.getAttribute("dashboardPath");
String dashboardPath = dashboardAttr instanceof String ? (String) dashboardAttr : (cp + "/login");
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>UniRide</title>
  <link rel="stylesheet" href="<%= cp %>/assets/css/common.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/home.css?v=20260515" />
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
            <% if (currentUser == null) { %>
              <a class="main-btn-outline" href="<%= cp %>/login">Log In</a>
            <% } else { %>
              <a class="main-btn-outline" href="<%= dashboardPath %>">Go to Dashboard</a>
            <% } %>
          </div>
        </div>

        <% if (currentUser == null) { %>
          <div class="search-card home-login-card home-auth-card">
            <header class="home-auth-card-header">
              <span class="home-auth-eyebrow">UniRide account</span>
              <h4 class="home-auth-title">Log in</h4>
              <p class="home-login-lead">Sign in with your @sjsu.edu account to search rides and manage your trips.</p>
            </header>
            <form class="home-login-form" method="post" action="<%= cp %>/login" novalidate>
              <div class="input-group">
                <label for="home-email">Email (@sjsu.edu only)</label>
                <input id="home-email" name="email" type="email" placeholder="you@sjsu.edu" title="Use your San José State University email (you@sjsu.edu)" pattern="[^@\s]+@sjsu\.edu" autocomplete="email" required />
              </div>
              <div class="input-group">
                <label for="home-password">Password</label>
                <input id="home-password" name="password" type="password" placeholder="Enter your password" autocomplete="current-password" required />
              </div>
              <button class="home-auth-submit" type="submit">Log in</button>
            </form>
            <a class="home-auth-admin-btn" href="<%= cp %>/admin">Login as Admin</a>
            <p class="home-login-signup">New to UniRide? <a href="<%= cp %>/signup">Create an account</a></p>
          </div>
        <% } else { %>
          <div class="search-card home-login-card home-auth-card">
            <header class="home-auth-card-header">
              <span class="home-auth-eyebrow">You&rsquo;re in</span>
              <h4 class="home-auth-title">Welcome back</h4>
              <p class="home-login-lead">You are signed in as <%= currentUser.getFirstName() %>.</p>
            </header>
            <a class="home-auth-submit home-login-dashboard" href="<%= dashboardPath %>">Go to Dashboard</a>
            <a class="home-auth-admin-btn home-auth-admin-btn--outline" href="<%= cp %>/admin">Login as Admin</a>
          </div>
        <% } %>
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
</body>
</html>
