package repository;

import db.DBConnection;
import model.RideReview;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data access for ride reviews and notes ({@code Reviews} table).
 */
public final class ReviewRepository {
    private ReviewRepository() {}

    /**
     * Adds a review/note when the user participated on a completed ride.
     *
     * @param userEmail authenticated reviewer email
     * @param rideId ride identifier
     * @param ratingStars optional 1–5 stars (may be null for note-only)
     * @param comments note text (required)
     * @return true when inserted
     */
    public static boolean submitReview(String userEmail, String rideId, Integer ratingStars, String comments) {
        if (userEmail == null || userEmail.isBlank() || rideId == null || rideId.isBlank()) {
            return false;
        }
        if (comments == null || comments.isBlank()) {
            return false;
        }
        if (ratingStars != null && (ratingStars < 1 || ratingStars > 5)) {
            return false;
        }

        try (Connection c = DBConnection.get()) {
            RideStatusStore.refreshRideStatuses(c);

            int ridePk = Integer.parseInt(rideId);
            String rideStatus;
            int driverId;

            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT r.Status, r.Driver_ID FROM Rides r WHERE r.Ride_ID = ?")) {
                ps.setInt(1, ridePk);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    rideStatus = rs.getString("Status");
                    driverId = rs.getInt("Driver_ID");
                }
            }

            if (!"completed".equalsIgnoreCase(rideStatus)) {
                return false;
            }

            int reviewerId;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT User_ID FROM Users WHERE Email = ? AND Account_Status = 'active' LIMIT 1")) {
                ps.setString(1, userEmail);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    reviewerId = rs.getInt("User_ID");
                }
            }

            if (!canParticipateOnRide(c, reviewerId, ridePk, driverId)) {
                return false;
            }

            if (hasReviewFromReviewer(c, ridePk, reviewerId)) {
                return false;
            }

            String insertSql =
                    "INSERT INTO Reviews (Ride_ID, Reviewer_ID, Driver_ID, Rating_Stars, Comments, Review_Date) " +
                    "VALUES (?, ?, ?, ?, ?, NOW())";

            try (PreparedStatement ps = c.prepareStatement(insertSql)) {
                ps.setInt(1, ridePk);
                ps.setInt(2, reviewerId);
                ps.setInt(3, driverId);
                if (ratingStars == null) {
                    ps.setNull(4, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(4, ratingStars);
                }
                ps.setString(5, comments.trim());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException | NumberFormatException e) {
            throw new RuntimeException("submitReview failed", e);
        }
    }

    /** Returns all reviews for one ride, oldest first. */
    public static List<RideReview> getReviewsForRide(String rideId) {
        if (rideId == null || rideId.isBlank()) {
            return List.of();
        }
        Map<String, List<RideReview>> map = getReviewsByRideIds(List.of(rideId));
        return map.getOrDefault(rideId, List.of());
    }

    /**
     * Loads reviews for many rides in one query.
     *
     * @param rideIds ride identifiers (null/blank entries ignored)
     * @return map keyed by ride id string
     */
    public static Map<String, List<RideReview>> getReviewsByRideIds(Collection<String> rideIds) {
        Map<String, List<RideReview>> result = new HashMap<>();
        if (rideIds == null || rideIds.isEmpty()) {
            return result;
        }

        Set<Integer> ids = new HashSet<>();
        for (String id : rideIds) {
            if (id == null || id.isBlank()) {
                continue;
            }
            try {
                ids.add(Integer.parseInt(id));
            } catch (NumberFormatException ignored) {
            }
        }
        if (ids.isEmpty()) {
            return result;
        }

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                placeholders.append(',');
            }
            placeholders.append('?');
        }

        String sql =
                "SELECT rv.Review_ID, rv.Ride_ID, rv.Rating_Stars, rv.Comments, rv.Review_Date, " +
                "       CONCAT(u.First_Name, ' ', LEFT(u.Last_Name, 1), '.') AS Reviewer_Name " +
                "FROM Reviews rv " +
                "JOIN Users u ON u.User_ID = rv.Reviewer_ID " +
                "WHERE rv.Ride_ID IN (" + placeholders + ") " +
                "ORDER BY rv.Ride_ID, rv.Review_Date ASC";

        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = 1;
            for (Integer id : ids) {
                ps.setInt(idx++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String rideKey = String.valueOf(rs.getInt("Ride_ID"));
                    Integer stars = rs.getObject("Rating_Stars", Integer.class);
                    result.computeIfAbsent(rideKey, k -> new ArrayList<>()).add(new RideReview(
                            String.valueOf(rs.getInt("Review_ID")),
                            rideKey,
                            rs.getString("Reviewer_Name"),
                            stars,
                            rs.getString("Comments"),
                            rs.getString("Review_Date")
                    ));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("getReviewsByRideIds failed", e);
        }
    }

    /**
     * Returns the current user's review per ride, if any.
     */
    public static Map<String, RideReview> getMyReviewsByRideIds(String userEmail, Collection<String> rideIds) {
        Map<String, RideReview> result = new HashMap<>();
        if (userEmail == null || userEmail.isBlank() || rideIds == null || rideIds.isEmpty()) {
            return result;
        }

        Set<Integer> ids = new HashSet<>();
        for (String id : rideIds) {
            if (id == null || id.isBlank()) {
                continue;
            }
            try {
                ids.add(Integer.parseInt(id));
            } catch (NumberFormatException ignored) {
            }
        }
        if (ids.isEmpty()) {
            return result;
        }

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                placeholders.append(',');
            }
            placeholders.append('?');
        }

        String sql =
                "SELECT rv.Review_ID, rv.Ride_ID, rv.Rating_Stars, rv.Comments, rv.Review_Date, " +
                "       CONCAT(u.First_Name, ' ', LEFT(u.Last_Name, 1), '.') AS Reviewer_Name " +
                "FROM Reviews rv " +
                "JOIN Users u ON u.User_ID = rv.Reviewer_ID " +
                "JOIN Users me ON me.User_ID = rv.Reviewer_ID AND me.Email = ? AND me.Account_Status = 'active' " +
                "WHERE rv.Ride_ID IN (" + placeholders + ")";

        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userEmail);
            int idx = 2;
            for (Integer id : ids) {
                ps.setInt(idx++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String rideKey = String.valueOf(rs.getInt("Ride_ID"));
                    Integer stars = rs.getObject("Rating_Stars", Integer.class);
                    result.put(rideKey, new RideReview(
                            String.valueOf(rs.getInt("Review_ID")),
                            rideKey,
                            rs.getString("Reviewer_Name"),
                            stars,
                            rs.getString("Comments"),
                            rs.getString("Review_Date")
                    ));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("getMyReviewsByRideIds failed", e);
        }
    }

    private static boolean canParticipateOnRide(Connection c, int userId, int rideId, int driverId)
            throws SQLException {
        if (userId == driverId) {
            return true;
        }
        String sql =
                "SELECT 1 FROM Bookings b " +
                "WHERE b.Ride_ID = ? AND b.User_ID = ? AND b.Status IN ('accepted', 'completed') LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, rideId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean hasReviewFromReviewer(Connection c, int rideId, int reviewerId) throws SQLException {
        String sql = "SELECT 1 FROM Reviews WHERE Ride_ID = ? AND Reviewer_ID = ? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, rideId);
            ps.setInt(2, reviewerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
