package com.lms.shared.view;

import java.util.List;

public record ModuleView(String id, String title, int sortOrder, List<LessonView> lessons) {
}
