package com.lms.catalog;

import com.lms.catalog.entity.CategoryEntity;
import com.lms.catalog.entity.CourseEntity;
import com.lms.catalog.entity.LessonEntity;
import com.lms.catalog.entity.ModuleEntity;
import com.lms.catalog.repo.CategoryRepository;
import com.lms.catalog.repo.CourseRepository;
import com.lms.catalog.repo.LessonRepository;
import com.lms.catalog.repo.ModuleRepository;
import com.lms.shared.AppException;
import com.lms.shared.CatalogPort;
import com.lms.shared.CourseCounts;
import com.lms.shared.CourseSnapshot;
import com.lms.shared.EventPublisher;
import com.lms.shared.Roles;
import com.lms.shared.UserSnapshot;
import com.lms.shared.event.CoursePublished;
import com.lms.shared.event.CourseRatingUpdated;
import com.lms.shared.input.AddLessonInput;
import com.lms.shared.input.AddModuleInput;
import com.lms.shared.input.CatalogFilterInput;
import com.lms.shared.input.CourseSortField;
import com.lms.shared.input.CourseSortInput;
import com.lms.shared.input.CreateCourseInput;
import com.lms.shared.input.SortDirection;
import com.lms.shared.view.CatalogSummaryView;
import com.lms.shared.view.CategoryView;
import com.lms.shared.view.CourseView;
import com.lms.shared.view.DifficultyCountView;
import com.lms.shared.view.LessonView;
import com.lms.shared.view.ModuleView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogService implements CatalogPort {
    private static final List<String> DIFFICULTIES = List.of("beginner", "intermediate", "advanced");
    private static final Set<String> CONTENT_TYPES = Set.of("text", "video", "quiz");

    private final CourseRepository courses;
    private final ModuleRepository modules;
    private final LessonRepository lessons;
    private final CategoryRepository categories;
    private final EventPublisher events;

    @Transactional(readOnly = true, transactionManager = "catalogTx")
    public List<CourseView> listPublished(CatalogFilterInput filter, CourseSortInput sort) {
        return filterPublished(filter).stream()
                .sorted(comparator(sort))
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true, transactionManager = "catalogTx")
    public CatalogSummaryView summary(CatalogFilterInput filter) {
        List<CourseEntity> found = filterPublished(filter);
        double avgRating = found.stream().mapToDouble(CourseEntity::getRating).average().orElse(0);
        List<DifficultyCountView> byDifficulty = DIFFICULTIES.stream()
                .map(d -> new DifficultyCountView(d, (int) found.stream().filter(c -> d.equals(c.getDifficulty())).count()))
                .collect(Collectors.toList());
        return new CatalogSummaryView(found.size(), round2(avgRating), byDifficulty);
    }

    @Transactional(readOnly = true, transactionManager = "catalogTx")
    public CourseView getCourseTree(String courseId) {
        return courses.findById(courseId).map(this::toView).orElse(null);
    }

    @Transactional(transactionManager = "catalogTx")
    public CourseView createCourse(UserSnapshot actor, CreateCourseInput input) {
        if (!actor.hasRole(Roles.TEACHER) && !actor.isAdmin()) {
            throw AppException.forbidden("Создавать курсы может только преподаватель");
        }
        CategoryEntity category = categories.findById(input.categoryId())
                .orElseThrow(() -> AppException.notFound("Категория не найдена"));
        String title = requireText(input.title(), "Укажите название курса");
        String difficulty = requireDifficulty(input.difficulty());
        CourseEntity course = courses.save(new CourseEntity(
                UUID.randomUUID().toString(), actor.id(), title, difficulty, "draft", 0, category
        ));
        return toView(course);
    }

    @Transactional(transactionManager = "catalogTx")
    public ModuleView addModule(UserSnapshot actor, AddModuleInput input) {
        CourseEntity course = courses.findById(input.courseId())
                .orElseThrow(() -> AppException.notFound("Курс не найден"));
        requireAuthor(actor, course);
        requireEditable(course);
        String title = requireText(input.title(), "Укажите название модуля");
        int sortOrder = sortOrderOrNext(input.sortOrder(), modules.countByCourse_Id(course.getId()));
        ModuleEntity module = modules.save(new ModuleEntity(UUID.randomUUID().toString(), title, sortOrder, course));
        return new ModuleView(module.getId(), module.getTitle(), module.getSortOrder(), List.of());
    }

    @Transactional(transactionManager = "catalogTx")
    public LessonView addLesson(UserSnapshot actor, AddLessonInput input) {
        ModuleEntity module = modules.findById(input.moduleId())
                .orElseThrow(() -> AppException.notFound("Модуль не найден"));
        CourseEntity course = module.getCourse();
        requireAuthor(actor, course);
        requireEditable(course);
        String title = requireText(input.title(), "Укажите название урока");
        String contentType = input.contentType() == null ? "" : input.contentType().trim();
        if (!CONTENT_TYPES.contains(contentType)) {
            throw AppException.badRequest("Неизвестный тип материала: " + contentType + ". Допустимо: text, video, quiz");
        }
        int sortOrder = sortOrderOrNext(input.sortOrder(), lessons.countByModule_Id(module.getId()));
        LessonEntity lesson = lessons.save(new LessonEntity(
                UUID.randomUUID().toString(), title, contentType, sortOrder, module
        ));
        return new LessonView(lesson.getId(), lesson.getTitle(), lesson.getContentType(), lesson.getSortOrder());
    }

    @Transactional(transactionManager = "catalogTx")
    public CourseView publishCourse(UserSnapshot actor, String courseId) {
        CourseEntity course = courses.findById(courseId)
                .orElseThrow(() -> AppException.notFound("Курс не найден"));
        requireAuthor(actor, course);
        if ("published".equals(course.getStatus())) {
            throw AppException.conflict("Курс уже опубликован");
        }
        if (!"draft".equals(course.getStatus())) {
            throw AppException.conflict("Опубликовать можно только черновик курса");
        }
        if (course.getModules().isEmpty()) {
            throw AppException.conflict("Нельзя опубликовать курс без модулей");
        }
        course.getModules().stream()
                .filter(m -> m.getLessons().isEmpty())
                .findFirst()
                .ifPresent(m -> {
                    throw AppException.conflict("В модуле «" + m.getTitle() + "» нет уроков");
                });
        course.setStatus("published");
        events.publish(new CoursePublished(course.getId(), course.getTitle(), course.getAuthorId()));
        return toView(course);
    }

    @Transactional(transactionManager = "catalogTx", propagation = Propagation.REQUIRES_NEW)
    public void handleRatingUpdated(CourseRatingUpdated event) {
        courses.findById(event.courseId()).ifPresent(c -> c.setRating(event.rating()));
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
    public String lessonCourseId(String lessonId) {
        return lessons.findById(lessonId)
                .map(l -> l.getModule().getCourse().getId())
                .orElse(null);
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
                    int moduleCount = c.getModules().size();
                    int lessonCount = c.getModules().stream().mapToInt(m -> m.getLessons().size()).sum();
                    return new CourseCounts(moduleCount, lessonCount);
                })
                .orElse(new CourseCounts(0, 0));
    }

    private List<CourseEntity> filterPublished(CatalogFilterInput filter) {
        String categoryId = filter == null ? null : blankToNull(filter.categoryId());
        String difficulty = filter == null ? null : blankToNull(filter.difficulty());
        String search = filter == null ? null : blankToNull(filter.search());
        Double minRating = filter == null ? null : filter.minRating();
        if (difficulty != null) {
            requireDifficulty(difficulty);
        }
        if (minRating != null && (minRating < 0 || minRating > 5)) {
            throw AppException.badRequest("minRating должен быть от 0 до 5");
        }
        String needle = search == null ? null : search.toLowerCase(Locale.ROOT);
        return courses.findByStatus("published").stream()
                .filter(c -> categoryId == null || categoryId.equals(c.getCategory().getId()))
                .filter(c -> difficulty == null || difficulty.equals(c.getDifficulty()))
                .filter(c -> needle == null || c.getTitle().toLowerCase(Locale.ROOT).contains(needle))
                .filter(c -> minRating == null || c.getRating() >= minRating)
                .collect(Collectors.toList());
    }

    private static Comparator<CourseEntity> comparator(CourseSortInput sort) {
        Comparator<CourseEntity> byTitle = Comparator.comparing(CourseEntity::getTitle, String.CASE_INSENSITIVE_ORDER);
        Comparator<CourseEntity> result = sort != null && sort.field() == CourseSortField.RATING
                ? Comparator.comparingDouble(CourseEntity::getRating).thenComparing(byTitle)
                : byTitle;
        return sort != null && sort.direction() == SortDirection.DESC ? result.reversed() : result;
    }

    private static void requireAuthor(UserSnapshot actor, CourseEntity course) {
        if (!actor.isAdmin() && !actor.id().equals(course.getAuthorId())) {
            throw AppException.forbidden("Изменять курс может только его автор");
        }
    }

    private static void requireEditable(CourseEntity course) {
        if ("archived".equals(course.getStatus())) {
            throw AppException.conflict("Архивный курс нельзя изменять");
        }
    }

    private static String requireDifficulty(String difficulty) {
        String value = difficulty == null ? "" : difficulty.trim();
        if (!DIFFICULTIES.contains(value)) {
            throw AppException.badRequest("Неизвестная сложность: " + value + ". Допустимо: beginner, intermediate, advanced");
        }
        return value;
    }

    private static String requireText(String value, String message) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw AppException.badRequest(message);
        }
        return text;
    }

    private static int sortOrderOrNext(Integer requested, long existing) {
        if (requested == null) {
            return (int) existing + 1;
        }
        if (requested < 1) {
            throw AppException.badRequest("Порядковый номер должен быть больше нуля");
        }
        return requested;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }

    private CourseSnapshot toSnapshot(CourseEntity c) {
        return new CourseSnapshot(
                c.getId(), c.getTitle(), c.getDifficulty(), c.getStatus(), c.getRating(),
                c.getCategory().getId(), c.getCategory().getName(), c.getAuthorId()
        );
    }

    private CourseView toView(CourseEntity c) {
        List<ModuleView> moduleViews = c.getModules().stream()
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
                moduleViews
        );
    }
}
