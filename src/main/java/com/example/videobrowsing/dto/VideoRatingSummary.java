package com.example.videobrowsing.dto;

public class VideoRatingSummary {

    private double averageRating;
    private long ratingCount;
    private Integer userRating;

    public VideoRatingSummary() {
    }

    public VideoRatingSummary(double averageRating, long ratingCount, Integer userRating) {
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
        this.userRating = userRating;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public long getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(long ratingCount) {
        this.ratingCount = ratingCount;
    }

    public Integer getUserRating() {
        return userRating;
    }

    public void setUserRating(Integer userRating) {
        this.userRating = userRating;
    }
}
