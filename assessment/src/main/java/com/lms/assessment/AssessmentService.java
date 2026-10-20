package com.lms.assessment;

import com.lms.assessment.entity.AttemptEntity;
import com.lms.assessment.entity.GradeEntity;
import com.lms.assessment.repo.AssignmentRepository;
import com.lms.assessment.repo.AttemptRepository;
import com.lms.shared.AppException;
import com.lms.shared.CatalogPort;
import com.lms.shared.EnrollmentPort;
import com.lms.shared.view.AssignmentView;
import com.lms.shared.view.AttemptView;
import com.lms.shared.view.GradeView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssessmentService {
    private final AssignmentRepository assignments;
    private final AttemptRepository attempts;
    private final CatalogPort catalog;
    private final EnrollmentPort enrollment;

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
    public List<AttemptView> attemptJournal(String studentId, String courseId) {
        if (catalog.getCourse(courseId) == null) {
            throw AppException.notFound("Курс не найден");
        }
        if (!enrollment.isEnrolled(studentId, courseId)) {
            throw AppException.forbidden("Студент не записан на курс");
        }
        List<String> lessonIds = catalog.listLessonIds(courseId);
        if (lessonIds.isEmpty()) {
            return List.of();
        }
        return attempts.findByStudentIdAndAssignmentLessonIdIn(studentId, lessonIds).stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    private AssignmentView toAssignment(com.lms.assessment.entity.AssignmentEntity a) {
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
