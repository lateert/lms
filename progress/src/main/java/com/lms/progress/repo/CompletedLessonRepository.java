package com.lms.progress.repo;

import com.lms.progress.entity.CompletedLessonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompletedLessonRepository extends JpaRepository<CompletedLessonEntity, String> {
    boolean existsByEnrollmentIdAndLessonId(String enrollmentId, String lessonId);

    long countByEnrollmentId(String enrollmentId);
}
