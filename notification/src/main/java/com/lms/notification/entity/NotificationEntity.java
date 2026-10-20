package com.lms.notification.entity;

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
@Table(name = "notification")
public class NotificationEntity {
    @Id
    private String id;
    @Column(name = "recipient_id", nullable = false)
    private String recipientId;
    @Column(nullable = false)
    private String type;
    @Column(length = 2000, nullable = false)
    private String text;
    @Column(name = "is_read", nullable = false)
    private boolean read;
    @Column(name = "created_at", nullable = false)
    private String createdAt;
}
