package com.lms.shared.event;

public record GradeAssigned(
        String attemptId,
        String studentId,
        String courseId,
        String assignmentTitle,
        double score,
        double maxScore
) {
}
