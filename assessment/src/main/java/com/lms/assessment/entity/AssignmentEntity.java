package com.lms.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "assignment")
public class AssignmentEntity {
    @Id
    private String id;
    @Column(name = "lesson_id", nullable = false)
    private String lessonId;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private String deadline;
    @Column(name = "max_score", nullable = false)
    private double maxScore;

    @OneToMany(mappedBy = "assignment")
    private List<AttemptEntity> attempts = new ArrayList<>();

    public AssignmentEntity(String id, String lessonId, String title, String deadline, double maxScore) {
        this.id = id;
        this.lessonId = lessonId;
        this.title = title;
        this.deadline = deadline;
        this.maxScore = maxScore;
    }
}
