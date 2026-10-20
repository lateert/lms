package com.lms.shared.view;

public record AttemptView(
        String id,
        String assignmentId,
        String studentId,
        String submittedAt,
        String content,
        String status,
        AssignmentView assignment,
        GradeView grade
) {
}
