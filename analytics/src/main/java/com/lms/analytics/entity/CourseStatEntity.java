package com.lms.analytics.entity;

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
@Table(name = "course_stat")
public class CourseStatEntity {
    @Id
    @Column(name = "course_id")
    private String courseId;
    @Column(name = "enroll_count", nullable = false)
    private int enrollCount;
    @Column(name = "avg_score", nullable = false)
    private double avgScore;
    @Column(name = "updated_at", nullable = false)
    private String updatedAt;
}
