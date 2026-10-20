package com.lms.shared.view;

public record CourseAnalyticsView(String courseId, int enrollCount, double avgScore, String updatedAt) {
}
