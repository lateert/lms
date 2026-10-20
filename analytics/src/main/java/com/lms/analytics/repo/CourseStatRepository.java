package com.lms.analytics.repo;

import com.lms.analytics.entity.CourseStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseStatRepository extends JpaRepository<CourseStatEntity, String> {
}
