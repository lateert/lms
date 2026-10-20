package com.lms.progress.repo;

import com.lms.progress.entity.ProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProgressRepository extends JpaRepository<ProgressEntity, String> {
    Optional<ProgressEntity> findByEnrollmentId(String enrollmentId);
}
