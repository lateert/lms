package com.lms.catalog.repo;

import com.lms.catalog.entity.ModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleRepository extends JpaRepository<ModuleEntity, String> {
    long countByCourse_Id(String courseId);
}
