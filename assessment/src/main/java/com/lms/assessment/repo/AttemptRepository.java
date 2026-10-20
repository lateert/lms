package com.lms.assessment.repo;

import com.lms.assessment.entity.AttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AttemptRepository extends JpaRepository<AttemptEntity, String> {
    List<AttemptEntity> findByStudentIdAndAssignmentLessonIdIn(String studentId, Collection<String> lessonIds);
}
