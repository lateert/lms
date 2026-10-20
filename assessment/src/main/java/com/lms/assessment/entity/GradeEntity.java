package com.lms.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
@Table(name = "grade")
public class GradeEntity {
    @Id
    private String id;
    @Column(name = "teacher_id", nullable = false)
    private String teacherId;
    @Column(nullable = false)
    private double score;
    @Column(length = 2000)
    private String comment;
    @Column(name = "graded_at", nullable = false)
    private String gradedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", unique = true, nullable = false)
    private AttemptEntity attempt;
}
