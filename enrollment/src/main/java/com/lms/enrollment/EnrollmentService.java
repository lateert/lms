package com.lms.enrollment;

import com.lms.enrollment.entity.EnrollmentEntity;
import com.lms.enrollment.repo.EnrollmentRepository;
import com.lms.shared.CatalogPort;
import com.lms.shared.EnrollmentPort;
import com.lms.shared.EnrollmentSnapshot;
import com.lms.shared.view.CourseView;
import com.lms.shared.view.EnrollmentView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService implements EnrollmentPort {
    private final EnrollmentRepository enrollments;
    private final CatalogPort catalog;

    @Override
    @Transactional(readOnly = true, transactionManager = "enrollmentTx")
    public boolean isEnrolled(String studentId, String courseId) {
        return find(studentId, courseId) != null;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "enrollmentTx")
    public EnrollmentSnapshot find(String studentId, String courseId) {
        return enrollments.findByStudentIdAndCourseId(studentId, courseId)
                .map(this::toSnapshot)
                .orElse(null);
    }

    @Transactional(readOnly = true, transactionManager = "enrollmentTx")
    public List<EnrollmentView> listForStudent(String studentId) {
        return enrollments.findByStudentId(studentId).stream()
                .map(e -> new EnrollmentView(
                        e.getId(),
                        e.getStudentId(),
                        e.getCourseId(),
                        e.getStatus(),
                        e.getEnrolledAt(),
                        CourseView.fromSnapshot(catalog.getPublishedCourse(e.getCourseId()))
                ))
                .collect(Collectors.toList());
    }

    private EnrollmentSnapshot toSnapshot(EnrollmentEntity e) {
        return new EnrollmentSnapshot(e.getId(), e.getStudentId(), e.getCourseId(), e.getStatus(), e.getEnrolledAt());
    }
}
