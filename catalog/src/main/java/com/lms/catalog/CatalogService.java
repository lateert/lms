package com.lms.catalog;

import com.lms.catalog.entity.CourseEntity;
import com.lms.catalog.entity.LessonEntity;
import com.lms.catalog.entity.ModuleEntity;
import com.lms.catalog.repo.CourseRepository;
import com.lms.catalog.repo.LessonRepository;
import com.lms.shared.CatalogPort;
import com.lms.shared.CourseCounts;
import com.lms.shared.CourseSnapshot;
import com.lms.shared.view.CategoryView;
import com.lms.shared.view.CourseView;
import com.lms.shared.view.LessonView;
import com.lms.shared.view.ModuleView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogService implements CatalogPort {
    private final CourseRepository courses;
    private final LessonRepository lessons;

    @Transactional(readOnly = true, transactionManager = "catalogTx")
    public List<CourseView> listPublished(String categoryId, String difficulty, String sortBy) {
        return courses.findByStatus("published").stream()
                .filter(c -> categoryId == null || categoryId.equals(c.getCategory().getId()))
                .filter(c -> difficulty == null || difficulty.isBlank() || difficulty.equals(c.getDifficulty()))
                .sorted(comparator(sortBy))
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true, transactionManager = "catalogTx")
    public CourseView getCourseTree(String courseId) {
        return courses.findById(courseId).map(this::toView).orElse(null);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "catalogTx")
    public CourseSnapshot getPublishedCourse(String courseId) {
        return courses.findById(courseId)
                .filter(c -> "published".equals(c.getStatus()))
                .map(this::toSnapshot)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "catalogTx")
    public CourseSnapshot getCourse(String courseId) {
        return courses.findById(courseId).map(this::toSnapshot).orElse(null);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "catalogTx")
    public boolean lessonExists(String lessonId) {
        return lessons.existsById(lessonId);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "catalogTx")
    public List<String> listLessonIds(String courseId) {
        return courses.findById(courseId)
                .map(c -> c.getModules().stream()
                        .flatMap(m -> m.getLessons().stream())
                        .map(LessonEntity::getId)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "catalogTx")
    public CourseCounts getCourseCounts(String courseId) {
        return courses.findById(courseId)
                .map(c -> {
                    int modules = c.getModules().size();
                    int lessonCount = c.getModules().stream().mapToInt(m -> m.getLessons().size()).sum();
                    return new CourseCounts(modules, lessonCount);
                })
                .orElse(new CourseCounts(0, 0));
    }

    private Comparator<CourseEntity> comparator(String sortBy) {
        if ("rating".equals(sortBy)) {
            return Comparator.comparingDouble(CourseEntity::getRating).reversed();
        }
        return Comparator.comparing(CourseEntity::getTitle);
    }

    private CourseSnapshot toSnapshot(CourseEntity c) {
        return new CourseSnapshot(
                c.getId(), c.getTitle(), c.getDifficulty(), c.getStatus(), c.getRating(),
                c.getCategory().getId(), c.getCategory().getName(), c.getAuthorId()
        );
    }

    private CourseView toView(CourseEntity c) {
        c.getModules().forEach(m -> m.getLessons().size());
        List<ModuleView> modules = c.getModules().stream()
                .sorted(Comparator.comparingInt(ModuleEntity::getSortOrder))
                .map(m -> new ModuleView(
                        m.getId(),
                        m.getTitle(),
                        m.getSortOrder(),
                        m.getLessons().stream()
                                .sorted(Comparator.comparingInt(LessonEntity::getSortOrder))
                                .map(l -> new LessonView(l.getId(), l.getTitle(), l.getContentType(), l.getSortOrder()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
        return new CourseView(
                c.getId(),
                c.getTitle(),
                c.getDifficulty(),
                c.getStatus(),
                c.getRating(),
                c.getAuthorId(),
                new CategoryView(c.getCategory().getId(), c.getCategory().getName(), c.getCategory().getDescription()),
                modules
        );
    }
}
