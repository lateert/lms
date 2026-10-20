package com.lms.shared.view;

import com.lms.shared.CourseSnapshot;

import java.util.List;

public record CourseView(
        String id,
        String title,
        String difficulty,
        String status,
        double rating,
        String authorId,
        CategoryView category,
        List<ModuleView> modules
) {
    public static CourseView fromSnapshot(CourseSnapshot snap) {
        if (snap == null) {
            return null;
        }
        return new CourseView(
                snap.id(),
                snap.title(),
                snap.difficulty(),
                snap.status(),
                snap.rating(),
                snap.authorId(),
                new CategoryView(snap.categoryId(), snap.categoryName(), null),
                List.of()
        );
    }
}
