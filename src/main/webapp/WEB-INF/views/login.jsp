<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
String cp = request.getContextPath();
String email = (String) request.getAttribute("email");
String errorEmail = (String) request.getAttribute("errorEmail");
String errorPassword = (String) request.getAttribute("errorPassword");
if (email == null) {
  email = "";
}
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Login | UniRide</title>
  <link rel="stylesheet" href="<%= cp %>/assets/css/common.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/home.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/dashboard.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/login.css?v=20260427" />
</head>
<body>
  <div class="login-page dashboard-page auth-page">
    <nav class="navbar">
      <h1 class="logo"><a href="<%= cp %>/home">Uni<span class="highlight">Ride</span></a></h1>
      <div class="nav-links">
        <a class="nav-btn-secondary" href="<%= cp %>/home">Back to Home</a>
      </div>
    </nav>

    <section class="login-hero auth-hero">
      <div class="auth-copy">
        <span class="campus-tag">Welcome back</span>
        <h2>Sign in to continue your rides.</h2>
        <p>
          Sign in with your <strong>@sjsu.edu</strong> email to find rides, manage your dashboard, and keep your trips organized.
        </p>
      </div>

      <div class="login-shell auth-shell">
        <header class="login-header">
          <h2>Log In to UniRide</h2>
        </header>

        <form class="login-form" method="post" action="<%= cp %>/login" novalidate>
          <label for="email">Email (@sjsu.edu only)</label>
          <input id="email" name="email" type="email" value="<%= email %>" placeholder="you@sjsu.edu" title="Use your San José State University email (you@sjsu.edu)" pattern="[^@\s]+@sjsu\.edu" autocomplete="email" required />
          <% if (errorEmail != null) { %><p class="field-error"><%= errorEmail %></p><% } %>

          <label for="password">Password</label>
          <input id="password" name="password" type="password" placeholder="Enter your password" />
          <% if (errorPassword != null) { %><p class="field-error"><%= errorPassword %></p><% } %>

          <button class="login-submit" type="submit">Log in</button>
        </form>
      </div>
    </section>

    <footer class="footer">
      <p>Copyright 2026 UniRide | SJSU Carpool System</p>
    </footer>
  </div>
</body>
</html>
