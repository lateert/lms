package com.lms.shared.view;

public record AttemptSummaryView(
        String courseId,
        int total,
        int pending,
        int graded,
        double averageScore,
        double averagePercent
) {
}
