package com.lms.progress.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "progress")
public class ProgressEntity {
    @Id
    private String id;
    @Column(name = "enrollment_id", nullable = false, unique = true)
    private String enrollmentId;
    @Column(nullable = false)
    private double percent;
    @Column(name = "updated_at", nullable = false)
    private String updatedAt;
}
