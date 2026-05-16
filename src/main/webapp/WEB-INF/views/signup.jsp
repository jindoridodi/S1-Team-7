<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
String cp = request.getContextPath();
String firstName     = (String) request.getAttribute("firstName");
String lastName      = (String) request.getAttribute("lastName");
String email         = (String) request.getAttribute("email");
String sjsuId        = (String) request.getAttribute("sjsuId");
String gender        = (String) request.getAttribute("gender");
String licenseNumber = (String) request.getAttribute("licenseNumber");
String successMessage = (String) request.getAttribute("successMessage");
Object rolesAttr = request.getAttribute("roles");
String[] roles = null;
if (rolesAttr instanceof String[]) {
  roles = (String[]) rolesAttr;
} else {
  roles = request.getParameterValues("roles");
}

if (firstName     == null) firstName     = "";
if (lastName      == null) lastName      = "";
if (email         == null) email         = "";
if (sjsuId        == null) sjsuId        = "";
if (gender        == null) gender        = "";
if (licenseNumber == null) licenseNumber = "";

boolean rolePassenger = false;
boolean roleDriver    = false;
if (roles != null) {
  for (String role : roles) {
    if ("passenger".equals(role)) rolePassenger = true;
    if ("driver".equals(role))    roleDriver    = true;
  }
}
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Signup | UniRide</title>
  <link rel="stylesheet" href="<%= cp %>/assets/css/common.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/home.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/dashboard.css?v=20260427" />
  <link rel="stylesheet" href="<%= cp %>/assets/css/signup.css?v=20260427" />
</head>
<body>
  <div class="signup-page dashboard-page auth-page">
    <nav class="navbar">
      <h1 class="logo"><a href="<%= cp %>/home">Uni<span class="highlight">Ride</span></a></h1>
      <div class="nav-links">
        <a class="nav-btn-secondary" href="<%= cp %>/home">Back to Home</a>
      </div>
    </nav>

    <section class="signup-hero auth-hero">
      <div class="auth-copy">
        <span class="campus-tag">Join UniRide</span>
        <h2>Create an account that matches your role.</h2>
        <p>
          Register once with your <strong>@sjsu.edu</strong> email to ride as a passenger, drive, or both.
          Your account stays consistent across the site.
        </p>
      </div>

      <div class="signup-shell auth-shell">
        <% if (successMessage != null) { %>
          <p class="signup-success"><%= successMessage %></p>
        <% } %>

        <header class="signup-header">
          <h2>Create Your UniRide Account</h2>
        </header>

        <form class="signup-form" method="post" action="<%= cp %>/signup" novalidate>
          <div class="name-row">
            <div class="name-field">
              <label for="firstName">First name</label>
              <input id="firstName" name="firstName" type="text" value="<%= firstName %>" placeholder="First name" />
              <% if (request.getAttribute("errorFirstName") != null) { %><p class="field-error"><%= request.getAttribute("errorFirstName") %></p><% } %>
            </div>
            <div class="name-field">
              <label for="lastName">Last name</label>
              <input id="lastName" name="lastName" type="text" value="<%= lastName %>" placeholder="Last name" />
              <% if (request.getAttribute("errorLastName") != null) { %><p class="field-error"><%= request.getAttribute("errorLastName") %></p><% } %>
            </div>
          </div>

          <label for="email">Email (@sjsu.edu only)</label>
          <input id="email" name="email" type="email" value="<%= email %>" placeholder="you@sjsu.edu" title="Use your San José State University email (you@sjsu.edu)" pattern="[^@\s]+@sjsu\.edu" autocomplete="email" />
          <% if (request.getAttribute("errorEmail") != null) { %><p class="field-error"><%= request.getAttribute("errorEmail") %></p><% } %>

          <label for="sjsuId">SJSU ID</label>
          <input id="sjsuId" name="sjsuId" type="text" inputmode="numeric" maxlength="9" value="<%= sjsuId %>" placeholder="9-digit SJSU ID" />
          <% if (request.getAttribute("errorSjsuId") != null) { %><p class="field-error"><%= request.getAttribute("errorSjsuId") %></p><% } %>

          <label for="gender">Gender</label>
          <select id="gender" name="gender">
            <option value="">Select gender</option>
            <option value="male"             <%= "male".equals(gender)             ? "selected" : "" %>>Male</option>
            <option value="female"           <%= "female".equals(gender)           ? "selected" : "" %>>Female</option>
            <option value="non-binary"       <%= "non-binary".equals(gender)       ? "selected" : "" %>>Non-binary</option>
            <option value="prefer-not-to-say" <%= "prefer-not-to-say".equals(gender) ? "selected" : "" %>>Prefer not to say</option>
          </select>
          <% if (request.getAttribute("errorGender") != null) { %><p class="field-error"><%= request.getAttribute("errorGender") %></p><% } %>

          <label for="password">Password</label>
          <input id="password" name="password" type="password" placeholder="At least 8 characters" />
          <% if (request.getAttribute("errorPassword") != null) { %><p class="field-error"><%= request.getAttribute("errorPassword") %></p><% } %>

          <fieldset class="role-selector" aria-label="Register as roles">
            <legend>Register as (select one or more)</legend>
            <label>
              <input type="checkbox" name="roles" value="passenger" <%= rolePassenger ? "checked" : "" %> />
              Passenger
            </label>
            <label>
              <input type="checkbox" id="driverCheckbox" name="roles" value="driver" <%= roleDriver ? "checked" : "" %> />
              Driver
            </label>
          </fieldset>
          <% if (request.getAttribute("errorRoles") != null) { %><p class="field-error"><%= request.getAttribute("errorRoles") %></p><% } %>

          <%-- License number field — shown only when Driver is selected --%>
          <div id="licenseField" class="<%= roleDriver ? "d-block" : "d-none" %>">
            <label for="licenseNumber">Driver's License Number</label>
            <input id="licenseNumber" name="licenseNumber" type="text"
                   value="<%= licenseNumber %>" placeholder="e.g. D1234567" />
            <% if (request.getAttribute("errorLicense") != null) { %>
              <p class="field-error"><%= request.getAttribute("errorLicense") %></p>
            <% } %>
          </div>

          <button class="signup-submit" type="submit">Create account</button>
        </form>
      </div>
    </section>

    <footer class="footer">
      <p>Copyright 2026 UniRide | SJSU Carpool System</p>
    </footer>
  </div>

  <script>
    const driverCheckbox = document.getElementById('driverCheckbox');
    const licenseField   = document.getElementById('licenseField');

    function syncLicenseFieldVisibility() {
      licenseField.style.display = driverCheckbox.checked ? 'block' : 'none';
    }

    syncLicenseFieldVisibility();
    driverCheckbox.addEventListener('change', syncLicenseFieldVisibility);
  </script>
</body>
</html>