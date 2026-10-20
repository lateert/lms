package com.lms.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "attempt")
public class AttemptEntity {
    @Id
    private String id;
    @Column(name = "student_id", nullable = false)
    private String studentId;
    @Column(name = "submitted_at", nullable = false)
    private String submittedAt;
    @Column(length = 4000, nullable = false)
    private String content;
    @Column(nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assignment_id", nullable = false)
    private AssignmentEntity assignment;

    @OneToOne(mappedBy = "attempt", fetch = FetchType.EAGER)
    private GradeEntity grade;

    public AttemptEntity(String id, String studentId, String submittedAt, String content,
                         String status, AssignmentEntity assignment) {
        this.id = id;
        this.studentId = studentId;
        this.submittedAt = submittedAt;
        this.content = content;
        this.status = status;
        this.assignment = assignment;
    }
}
