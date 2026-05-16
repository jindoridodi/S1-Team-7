<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, java.util.Map, model.RideReview" %>
<%
// Required page scope: rideId, cp, reviewFormAction, isDriver (Boolean)
String sectionRideId = (String) request.getAttribute("_reviewRideId");
if (sectionRideId == null) sectionRideId = (String) pageContext.getAttribute("rideId");
Boolean isDriverRole = (Boolean) request.getAttribute("_reviewIsDriver");
if (isDriverRole == null) isDriverRole = Boolean.FALSE;
String formAction = (String) request.getAttribute("_reviewFormAction");
if (formAction == null) formAction = (String) pageContext.getAttribute("reviewFormAction");

@SuppressWarnings("unchecked")
Map<String, List<RideReview>> reviewsByRideId = (Map<String, List<RideReview>>) request.getAttribute("reviewsByRideId");
@SuppressWarnings("unchecked")
Map<String, RideReview> myReviewsByRideId = (Map<String, RideReview>) request.getAttribute("myReviewsByRideId");
if (reviewsByRideId == null) reviewsByRideId = java.util.Collections.emptyMap();
if (myReviewsByRideId == null) myReviewsByRideId = java.util.Collections.emptyMap();

List<RideReview> rideReviews = reviewsByRideId.getOrDefault(sectionRideId, java.util.Collections.emptyList());
RideReview myReview = myReviewsByRideId.get(sectionRideId);
%>
<div class="ride-reviews">
  <h4 class="ride-reviews-title">Ride notes &amp; reviews</h4>

  <% if (!rideReviews.isEmpty()) { %>
    <ul class="ride-reviews-list">
      <% for (RideReview rev : rideReviews) { %>
        <li class="ride-reviews-item">
          <strong><%= rev.getReviewerName() %></strong>
          <% if (rev.getRatingStars() != null) { %>
            <span class="ride-reviews-stars"><%= rev.getRatingStars() %>/5</span>
          <% } %>
          <p class="ride-reviews-comment"><%= rev.getComments() %></p>
          <% if (rev.getReviewDate() != null) { %>
            <small class="ride-meta"><%= rev.getReviewDate() %></small>
          <% } %>
        </li>
      <% } %>
    </ul>
  <% } else { %>
    <p class="ride-meta">No notes yet for this ride.</p>
  <% } %>

  <% if (myReview != null) { %>
    <p class="ride-meta ride-meta--success">You already left a note on this ride.</p>
  <% } else { %>
    <form method="post" action="<%= formAction %>" class="ride-review-form">
      <input type="hidden" name="action" value="submitReview" />
      <input type="hidden" name="rideId" value="<%= sectionRideId %>" />
      <% if (!isDriverRole) { %>
        <label class="ride-review-label">
          Rating (1–5)
          <select name="ratingStars" class="login-input" required>
            <option value="">Select</option>
            <option value="5">5 – Excellent</option>
            <option value="4">4 – Good</option>
            <option value="3">3 – OK</option>
            <option value="2">2 – Fair</option>
            <option value="1">1 – Poor</option>
          </select>
        </label>
      <% } else { %>
        <label class="ride-review-label">
          Rating (optional)
          <select name="ratingStars" class="login-input">
            <option value="">No rating</option>
            <option value="5">5</option>
            <option value="4">4</option>
            <option value="3">3</option>
            <option value="2">2</option>
            <option value="1">1</option>
          </select>
        </label>
      <% } %>
      <label class="ride-review-label">
        Note
        <textarea name="comments" class="login-input ride-review-textarea" rows="3" maxlength="500" placeholder="Share feedback for your <%= isDriverRole ? "passenger" : "driver" %>…" required></textarea>
      </label>
      <button type="submit" class="request-approve">Post note</button>
    </form>
  <% } %>
</div>
