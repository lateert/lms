package com.lms.assessment.repo;

import com.lms.assessment.entity.GradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeRepository extends JpaRepository<GradeEntity, String> {
}
