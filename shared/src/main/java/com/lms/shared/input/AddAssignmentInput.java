package com.lms.shared.input;

public record AddAssignmentInput(String lessonId, String title, String deadline, double maxScore) {
}
