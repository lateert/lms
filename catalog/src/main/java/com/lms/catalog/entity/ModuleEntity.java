package com.lms.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "module")
public class ModuleEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String title;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private CourseEntity course;

    @OneToMany(mappedBy = "module")
    private List<LessonEntity> lessons = new ArrayList<>();

    public ModuleEntity(String id, String title, int sortOrder, CourseEntity course) {
        this.id = id;
        this.title = title;
        this.sortOrder = sortOrder;
        this.course = course;
    }
}
