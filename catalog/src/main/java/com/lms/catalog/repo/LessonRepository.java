package com.lms.catalog.repo;

import com.lms.catalog.entity.LessonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<LessonEntity, String> {
    long countByModule_Id(String moduleId);
}
