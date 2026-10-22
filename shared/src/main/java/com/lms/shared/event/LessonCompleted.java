package com.lms.shared.event;

public record LessonCompleted(String enrollmentId, String studentId, String courseId, String lessonId, double percent) {
}
