package com.lms.catalog.repo;

import com.lms.catalog.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<CourseEntity, String> {
    List<CourseEntity> findByStatus(String status);
}
