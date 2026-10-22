package com.lms.shared;

import java.util.List;

public interface CatalogPort {
    CourseSnapshot getPublishedCourse(String courseId);

    CourseSnapshot getCourse(String courseId);

    boolean lessonExists(String lessonId);

    String lessonCourseId(String lessonId);

    List<String> listLessonIds(String courseId);

    CourseCounts getCourseCounts(String courseId);
}
