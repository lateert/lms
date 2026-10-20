package com.lms.shared.view;

public record EnrollmentView(
        String id,
        String studentId,
        String courseId,
        String status,
        String enrolledAt,
        CourseView course
) {
}
