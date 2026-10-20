package com.lms.enrollment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "enrollment", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"}))
public class EnrollmentEntity {
    @Id
    private String id;
    @Column(name = "student_id", nullable = false)
    private String studentId;
    @Column(name = "course_id", nullable = false)
    private String courseId;
    @Column(name = "enrolled_at", nullable = false)
    private String enrolledAt;
    @Column(nullable = false)
    private String status;
}
