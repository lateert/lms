package com.lms.assessment;

import com.lms.assessment.entity.AssignmentEntity;
import com.lms.assessment.entity.AttemptEntity;
import com.lms.assessment.entity.GradeEntity;
import com.lms.assessment.repo.AssignmentRepository;
import com.lms.assessment.repo.AttemptRepository;
import com.lms.assessment.repo.GradeRepository;
import com.lms.shared.AppException;
import com.lms.shared.CatalogPort;
import com.lms.shared.CourseSnapshot;
import com.lms.shared.EnrollmentPort;
import com.lms.shared.EventPublisher;
import com.lms.shared.Times;
import com.lms.shared.UserSnapshot;
import com.lms.shared.event.AssignmentSubmitted;
import com.lms.shared.event.GradeAssigned;
import com.lms.shared.input.AddAssignmentInput;
import com.lms.shared.input.AttemptFilterInput;
import com.lms.shared.input.GradeAttemptInput;
import com.lms.shared.input.SortDirection;
import com.lms.shared.input.SubmitAttemptInput;
import com.lms.shared.view.AssignmentView;
import com.lms.shared.view.AttemptSummaryView;
import com.lms.shared.view.AttemptView;
import com.lms.shared.view.GradeView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssessmentService {
    private static final Set<String> ATTEMPT_STATUSES = Set.of("submitted", "graded");
    private static final int MAX_CONTENT = 4000;

    private final AssignmentRepository assignments;
    private final AttemptRepository attempts;
    private final GradeRepository grades;
    private final CatalogPort catalog;
    private final EnrollmentPort enrollment;
    private final EventPublisher events;

    @Transactional(readOnly = true, transactionManager = "assessmentTx")
    public List<AssignmentView> listAssignments(String courseId) {
        List<String> lessonIds = catalog.listLessonIds(courseId);
        if (lessonIds.isEmpty()) {
            return List.of();
        }
        return assignments.findByLessonIdIn(lessonIds).stream()
                .filter(a -> catalog.lessonExists(a.getLessonId()))
                .map(this::toAssignment)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true, transactionManager = "assessmentTx")
    public List<AttemptView> attemptJournal(UserSnapshot actor, String courseId,
                                            AttemptFilterInput filter, SortDirection order) {
        String status = filter == null ? null : blankToNull(filter.status());
        String assignmentId = filter == null ? null : blankToNull(filter.assignmentId());
        String studentId = filter == null ? null : blankToNull(filter.studentId());
        if (status != null && !ATTEMPT_STATUSES.contains(status)) {
            throw AppException.badRequest("Неизвестный статус попытки: " + status + ". Допустимо: submitted, graded");
        }
        Comparator<AttemptEntity> bySubmitted = Comparator.comparing(AttemptEntity::getSubmittedAt);
        return visibleAttempts(actor, courseId).stream()
                .filter(a -> status == null || status.equals(a.getStatus()))
                .filter(a -> assignmentId == null || assignmentId.equals(a.getAssignment().getId()))
                .filter(a -> studentId == null || studentId.equals(a.getStudentId()))
                .sorted(order == SortDirection.ASC ? bySubmitted : bySubmitted.reversed())
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true, transactionManager = "assessmentTx")
    public AttemptSummaryView attemptSummary(UserSnapshot actor, String courseId) {
        List<AttemptEntity> rows = visibleAttempts(actor, courseId);
        List<AttemptEntity> graded = rows.stream()
                .filter(a -> "graded".equals(a.getStatus()) && a.getGrade() != null)
                .collect(Collectors.toList());
        int pending = (int) rows.stream().filter(a -> "submitted".equals(a.getStatus())).count();
        double averageScore = graded.stream().mapToDouble(a -> a.getGrade().getScore()).average().orElse(0);
        double averagePercent = graded.stream()
                .mapToDouble(a -> a.getGrade().getScore() * 100 / a.getAssignment().getMaxScore())
                .average()
                .orElse(0);
        return new AttemptSummaryView(courseId, rows.size(), pending, graded.size(),
                round2(averageScore), round2(averagePercent));
    }

    @Transactional(transactionManager = "assessmentTx")
    public AssignmentView addAssignment(UserSnapshot actor, AddAssignmentInput input) {
        CourseSnapshot course = courseOfLesson(input.lessonId());
        requireCourseOwner(actor, course, "Добавлять задания может только автор курса");
        String title = input.title() == null ? "" : input.title().trim();
        if (title.isEmpty()) {
            throw AppException.badRequest("Укажите название задания");
        }
        if (input.maxScore() <= 0 || input.maxScore() > 1000) {
            throw AppException.badRequest("Максимальный балл должен быть от 0 до 1000");
        }
        LocalDateTime deadline = Times.parse(input.deadline(), "Срок сдачи укажите в формате 2026-12-01T23:59:00");
        if (!deadline.isAfter(LocalDateTime.now())) {
            throw AppException.badRequest("Срок сдачи должен быть в будущем");
        }
        AssignmentEntity assignment = assignments.save(new AssignmentEntity(
                UUID.randomUUID().toString(), input.lessonId(), title, Times.format(deadline), input.maxScore()
        ));
        return toAssignment(assignment);
    }

    @Transactional(transactionManager = "assessmentTx")
    public AttemptView submitAttempt(UserSnapshot actor, SubmitAttemptInput input) {
        AssignmentEntity assignment = assignments.findById(input.assignmentId())
                .orElseThrow(() -> AppException.notFound("Задание не найдено"));
        String content = input.content() == null ? "" : input.content().trim();
        if (content.isEmpty()) {
            throw AppException.badRequest("Работа не может быть пустой");
        }
        if (content.length() > MAX_CONTENT) {
            throw AppException.badRequest("Работа длиннее " + MAX_CONTENT + " символов");
        }
        CourseSnapshot course = courseOfLesson(assignment.getLessonId());
        if (!enrollment.isEnrolled(actor.id(), course.id())) {
            throw AppException.forbidden("Студент не записан на курс");
        }
        if (LocalDateTime.parse(assignment.getDeadline()).isBefore(LocalDateTime.now())) {
            throw AppException.conflict("Срок сдачи задания истёк");
        }
        if (attempts.existsByStudentIdAndAssignment_IdAndStatus(actor.id(), assignment.getId(), "submitted")) {
            throw AppException.conflict("Предыдущая попытка ещё не проверена");
        }
        AttemptEntity attempt = attempts.save(new AttemptEntity(
                UUID.randomUUID().toString(), actor.id(), Times.now(), content, "submitted", assignment
        ));
        events.publish(new AssignmentSubmitted(
                attempt.getId(), assignment.getId(), assignment.getTitle(), actor.id(), course.id(), course.authorId()
        ));
        return toView(attempt);
    }

    @Transactional(transactionManager = "assessmentTx")
    public AttemptView gradeAttempt(UserSnapshot actor, GradeAttemptInput input) {
        AttemptEntity attempt = attempts.findById(input.attemptId())
                .orElseThrow(() -> AppException.notFound("Попытка не найдена"));
        AssignmentEntity assignment = attempt.getAssignment();
        CourseSnapshot course = courseOfLesson(assignment.getLessonId());
        requireCourseOwner(actor, course, "Оценивать работы может только автор курса");
        if ("graded".equals(attempt.getStatus())) {
            throw AppException.conflict("Попытка уже оценена");
        }
        if (input.score() < 0 || input.score() > assignment.getMaxScore()) {
            throw AppException.badRequest("Балл должен быть от 0 до " + formatScore(assignment.getMaxScore()));
        }
        String comment = input.comment() == null ? "" : input.comment().trim();
        GradeEntity grade = grades.save(new GradeEntity(
                UUID.randomUUID().toString(), actor.id(), input.score(), comment, Times.now(), attempt
        ));
        attempt.setGrade(grade);
        attempt.setStatus("graded");
        events.publish(new GradeAssigned(
                attempt.getId(), attempt.getStudentId(), course.id(), assignment.getTitle(),
                input.score(), assignment.getMaxScore()
        ));
        return toView(attempt);
    }

    private List<AttemptEntity> visibleAttempts(UserSnapshot actor, String courseId) {
        CourseSnapshot course = catalog.getCourse(courseId);
        if (course == null) {
            throw AppException.notFound("Курс не найден");
        }
        List<String> lessonIds = catalog.listLessonIds(courseId);
        if (isCourseOwner(actor, course)) {
            return lessonIds.isEmpty() ? List.of() : attempts.findByAssignmentLessonIdIn(lessonIds);
        }
        if (!enrollment.isEnrolled(actor.id(), courseId)) {
            throw AppException.forbidden("Студент не записан на курс");
        }
        return lessonIds.isEmpty() ? List.of() : attempts.findByStudentIdAndAssignmentLessonIdIn(actor.id(), lessonIds);
    }

    private CourseSnapshot courseOfLesson(String lessonId) {
        String courseId = catalog.lessonCourseId(lessonId);
        if (courseId == null) {
            throw AppException.notFound("Урок не найден");
        }
        return catalog.getCourse(courseId);
    }

    private static boolean isCourseOwner(UserSnapshot actor, CourseSnapshot course) {
        return actor.isAdmin() || actor.id().equals(course.authorId());
    }

    private static void requireCourseOwner(UserSnapshot actor, CourseSnapshot course, String message) {
        if (!isCourseOwner(actor, course)) {
            throw AppException.forbidden(message);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }

    private static String formatScore(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private AssignmentView toAssignment(AssignmentEntity a) {
        return new AssignmentView(a.getId(), a.getLessonId(), a.getTitle(), a.getDeadline(), a.getMaxScore());
    }

    private AttemptView toView(AttemptEntity att) {
        GradeEntity g = att.getGrade();
        GradeView grade = g == null ? null : new GradeView(
                g.getId(), att.getId(), g.getTeacherId(), g.getScore(), g.getComment(), g.getGradedAt()
        );
        return new AttemptView(
                att.getId(),
                att.getAssignment().getId(),
                att.getStudentId(),
                att.getSubmittedAt(),
                att.getContent(),
                att.getStatus(),
                toAssignment(att.getAssignment()),
                grade
        );
    }
}
