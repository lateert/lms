package com.lms.shared;

public record EnrollmentSnapshot(
        String id,
        String studentId,
        String courseId,
        String status,
        String enrolledAt
) {
}
