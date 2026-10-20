package com.lms.notification.repo;

import com.lms.notification.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {
    List<NotificationEntity> findByRecipientIdOrderByCreatedAtDesc(String recipientId);
}
