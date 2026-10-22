package com.lms.shared.event;

public record StudentEnrolled(String enrollmentId, String studentId, String courseId, String courseTitle) {
}
