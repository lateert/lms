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
@Table(name = "course")
public class CourseEntity {
    @Id
    private String id;
    @Column(name = "author_id", nullable = false)
    private String authorId;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private String difficulty;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private double rating;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @OneToMany(mappedBy = "course")
    private List<ModuleEntity> modules = new ArrayList<>();

    public CourseEntity(String id, String authorId, String title, String difficulty,
                        String status, double rating, CategoryEntity category) {
        this.id = id;
        this.authorId = authorId;
        this.title = title;
        this.difficulty = difficulty;
        this.status = status;
        this.rating = rating;
        this.category = category;
    }
}
