package com.lms.shared.event;

public record AssignmentSubmitted(
        String attemptId,
        String assignmentId,
        String assignmentTitle,
        String studentId,
        String courseId,
        String teacherId
) {
}
