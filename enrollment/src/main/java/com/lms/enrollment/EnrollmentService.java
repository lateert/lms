package com.lms.enrollment;

import com.lms.enrollment.entity.EnrollmentEntity;
import com.lms.enrollment.repo.EnrollmentRepository;
import com.lms.shared.AppException;
import com.lms.shared.CatalogPort;
import com.lms.shared.CourseSnapshot;
import com.lms.shared.EnrollmentPort;
import com.lms.shared.EnrollmentSnapshot;
import com.lms.shared.EventPublisher;
import com.lms.shared.Roles;
import com.lms.shared.Times;
import com.lms.shared.UserSnapshot;
import com.lms.shared.event.LessonCompleted;
import com.lms.shared.event.StudentEnrolled;
import com.lms.shared.view.CourseView;
import com.lms.shared.view.EnrollmentView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService implements EnrollmentPort {
    private static final Set<String> STATUSES = Set.of("active", "completed", "dropped");

    private final EnrollmentRepository enrollments;
    private final CatalogPort catalog;
    private final EventPublisher events;

    @Override
    @Transactional(readOnly = true, transactionManager = "enrollmentTx")
    public boolean isEnrolled(String studentId, String courseId) {
        EnrollmentSnapshot enrollment = find(studentId, courseId);
        return enrollment != null && !"dropped".equals(enrollment.status());
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "enrollmentTx")
    public EnrollmentSnapshot find(String studentId, String courseId) {
        return enrollments.findByStudentIdAndCourseId(studentId, courseId)
                .map(this::toSnapshot)
                .orElse(null);
    }

    @Transactional(readOnly = true, transactionManager = "enrollmentTx")
    public List<EnrollmentView> listForStudent(String studentId, String status) {
        String wanted = status == null || status.isBlank() ? null : status.trim();
        if (wanted != null && !STATUSES.contains(wanted)) {
            throw AppException.badRequest("Неизвестный статус записи: " + wanted + ". Допустимо: active, completed, dropped");
        }
        return enrollments.findByStudentId(studentId).stream()
                .filter(e -> wanted == null || wanted.equals(e.getStatus()))
                .sorted(Comparator.comparing(EnrollmentEntity::getEnrolledAt).reversed())
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Transactional(transactionManager = "enrollmentTx")
    public EnrollmentView enroll(UserSnapshot actor, String courseId) {
        if (!actor.hasRole(Roles.STUDENT)) {
            throw AppException.forbidden("Записаться на курс может только студент");
        }
        CourseSnapshot course = catalog.getPublishedCourse(courseId);
        if (course == null) {
            if (catalog.getCourse(courseId) == null) {
                throw AppException.notFound("Курс не найден");
            }
            throw AppException.conflict("Курс ещё не опубликован");
        }
        Optional<EnrollmentEntity> existing = enrollments.findByStudentIdAndCourseId(actor.id(), courseId);
        if (existing.isPresent()) {
            EnrollmentEntity enrollment = existing.get();
            if (!"dropped".equals(enrollment.getStatus())) {
                throw AppException.conflict("Студент уже записан на курс");
            }
            enrollment.setStatus("active");
            return toView(enrollment);
        }
        EnrollmentEntity enrollment = enrollments.save(new EnrollmentEntity(
                UUID.randomUUID().toString(), actor.id(), courseId, Times.now(), "active"
        ));
        events.publish(new StudentEnrolled(enrollment.getId(), actor.id(), courseId, course.title()));
        return toView(enrollment);
    }

    @Transactional(transactionManager = "enrollmentTx")
    public EnrollmentView drop(UserSnapshot actor, String courseId) {
        EnrollmentEntity enrollment = enrollments.findByStudentIdAndCourseId(actor.id(), courseId)
                .orElseThrow(() -> AppException.notFound("Запись на курс не найдена"));
        if (!"active".equals(enrollment.getStatus())) {
            throw AppException.conflict("Отменить можно только активную запись");
        }
        enrollment.setStatus("dropped");
        return toView(enrollment);
    }

    @Transactional(transactionManager = "enrollmentTx", propagation = Propagation.REQUIRES_NEW)
    public void handleLessonCompleted(LessonCompleted event) {
        if (event.percent() < 100) {
            return;
        }
        enrollments.findById(event.enrollmentId())
                .filter(e -> "active".equals(e.getStatus()))
                .ifPresent(e -> e.setStatus("completed"));
    }

    private EnrollmentView toView(EnrollmentEntity e) {
        return new EnrollmentView(
                e.getId(),
                e.getStudentId(),
                e.getCourseId(),
                e.getStatus(),
                e.getEnrolledAt(),
                CourseView.fromSnapshot(catalog.getCourse(e.getCourseId()))
        );
    }

    private EnrollmentSnapshot toSnapshot(EnrollmentEntity e) {
        return new EnrollmentSnapshot(e.getId(), e.getStudentId(), e.getCourseId(), e.getStatus(), e.getEnrolledAt());
    }
}
