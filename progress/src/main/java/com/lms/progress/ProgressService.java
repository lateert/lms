package com.lms.progress;

import com.lms.progress.entity.CompletedLessonEntity;
import com.lms.progress.entity.ProgressEntity;
import com.lms.progress.repo.CompletedLessonRepository;
import com.lms.progress.repo.ProgressRepository;
import com.lms.shared.AppException;
import com.lms.shared.CatalogPort;
import com.lms.shared.EnrollmentPort;
import com.lms.shared.EnrollmentSnapshot;
import com.lms.shared.EventPublisher;
import com.lms.shared.Times;
import com.lms.shared.UserSnapshot;
import com.lms.shared.event.GradeAssigned;
import com.lms.shared.event.LessonCompleted;
import com.lms.shared.event.StudentEnrolled;
import com.lms.shared.view.ProgressView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgressService {
    private final ProgressRepository progress;
    private final CompletedLessonRepository completedLessons;
    private final EnrollmentPort enrollment;
    private final CatalogPort catalog;
    private final EventPublisher events;

    @Transactional(readOnly = true, transactionManager = "progressTx")
    public ProgressView getForCourse(String studentId, String courseId) {
        EnrollmentSnapshot enr = enrollment.find(studentId, courseId);
        if (enr == null) {
            throw AppException.notFound("Прогресс по курсу не найден");
        }
        ProgressView view = findByEnrollmentId(enr.id());
        if (view == null) {
            throw AppException.notFound("Прогресс по курсу не найден");
        }
        return view;
    }

    @Transactional(readOnly = true, transactionManager = "progressTx")
    public ProgressView findByEnrollmentId(String enrollmentId) {
        return progress.findByEnrollmentId(enrollmentId).map(this::toView).orElse(null);
    }

    @Transactional(transactionManager = "progressTx")
    public ProgressView completeLesson(UserSnapshot actor, String lessonId) {
        String courseId = catalog.lessonCourseId(lessonId);
        if (courseId == null) {
            throw AppException.notFound("Урок не найден");
        }
        EnrollmentSnapshot enr = enrollment.find(actor.id(), courseId);
        if (enr == null || "dropped".equals(enr.status())) {
            throw AppException.forbidden("Студент не записан на курс");
        }
        if (completedLessons.existsByEnrollmentIdAndLessonId(enr.id(), lessonId)) {
            throw AppException.conflict("Урок уже отмечен как пройденный");
        }
        completedLessons.save(new CompletedLessonEntity(UUID.randomUUID().toString(), enr.id(), lessonId, Times.now()));
        ProgressEntity row = recalculate(enr.id(), courseId);
        events.publish(new LessonCompleted(enr.id(), actor.id(), courseId, lessonId, row.getPercent()));
        return toView(row);
    }

    @Transactional(transactionManager = "progressTx", propagation = Propagation.REQUIRES_NEW)
    public void handleStudentEnrolled(StudentEnrolled event) {
        if (progress.findByEnrollmentId(event.enrollmentId()).isEmpty()) {
            progress.save(new ProgressEntity(UUID.randomUUID().toString(), event.enrollmentId(), 0, Times.now()));
        }
    }

    @Transactional(transactionManager = "progressTx", propagation = Propagation.REQUIRES_NEW)
    public void handleGradeAssigned(GradeAssigned event) {
        EnrollmentSnapshot enr = enrollment.find(event.studentId(), event.courseId());
        if (enr != null) {
            recalculate(enr.id(), event.courseId());
        }
    }

    private ProgressEntity recalculate(String enrollmentId, String courseId) {
        ProgressEntity row = progress.findByEnrollmentId(enrollmentId)
                .orElseGet(() -> new ProgressEntity(UUID.randomUUID().toString(), enrollmentId, 0, Times.now()));
        int lessonCount = catalog.getCourseCounts(courseId).lessonCount();
        long done = completedLessons.countByEnrollmentId(enrollmentId);
        double percent = lessonCount == 0 ? 0 : Math.min(100.0, Math.round(done * 1000.0 / lessonCount) / 10.0);
        row.setPercent(percent);
        row.setUpdatedAt(Times.now());
        return progress.save(row);
    }

    private ProgressView toView(ProgressEntity row) {
        return new ProgressView(row.getId(), row.getEnrollmentId(), row.getPercent(), row.getUpdatedAt());
    }
}
