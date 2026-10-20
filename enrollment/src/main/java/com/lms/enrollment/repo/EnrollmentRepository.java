package com.lms.enrollment.repo;

import com.lms.enrollment.entity.EnrollmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, String> {
    List<EnrollmentEntity> findByStudentId(String studentId);

    Optional<EnrollmentEntity> findByStudentIdAndCourseId(String studentId, String courseId);
}
