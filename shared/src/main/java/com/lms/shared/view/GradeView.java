package com.lms.shared.view;

public record GradeView(
        String id,
        String attemptId,
        String teacherId,
        double score,
        String comment,
        String gradedAt
) {
}
