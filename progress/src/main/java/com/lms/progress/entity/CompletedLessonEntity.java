package com.lms.progress.entity;

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
@Table(name = "completed_lesson", uniqueConstraints = @UniqueConstraint(columnNames = {"enrollment_id", "lesson_id"}))
public class CompletedLessonEntity {
    @Id
    private String id;
    @Column(name = "enrollment_id", nullable = false)
    private String enrollmentId;
    @Column(name = "lesson_id", nullable = false)
    private String lessonId;
    @Column(name = "completed_at", nullable = false)
    private String completedAt;
}
