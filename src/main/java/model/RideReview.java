package model;

/**
 * A rating or note left by a passenger or driver for a completed ride.
 */
public class RideReview {
    private final String id;
    private final String rideId;
    private final String reviewerName;
    private final Integer ratingStars;
    private final String comments;
    private final String reviewDate;

    public RideReview(
            String id,
            String rideId,
            String reviewerName,
            Integer ratingStars,
            String comments,
            String reviewDate
    ) {
        this.id = id;
        this.rideId = rideId;
        this.reviewerName = reviewerName;
        this.ratingStars = ratingStars;
        this.comments = comments;
        this.reviewDate = reviewDate;
    }

    public String getId() { return id; }
    public String getRideId() { return rideId; }
    public String getReviewerName() { return reviewerName; }
    public Integer getRatingStars() { return ratingStars; }
    public String getComments() { return comments; }
    public String getReviewDate() { return reviewDate; }
}
