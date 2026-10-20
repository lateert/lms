package com.lms.shared;

public record CourseSnapshot(
        String id,
        String title,
        String difficulty,
        String status,
        double rating,
        String categoryId,
        String categoryName,
        String authorId
) {
}
