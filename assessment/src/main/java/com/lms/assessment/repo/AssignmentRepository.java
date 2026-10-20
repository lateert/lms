package com.lms.assessment.repo;

import com.lms.assessment.entity.AssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<AssignmentEntity, String> {
    List<AssignmentEntity> findByLessonIdIn(Collection<String> lessonIds);
}
