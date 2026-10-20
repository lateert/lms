package com.lms.notification;

import com.lms.shared.view.NotificationView;
import com.lms.notification.repo.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notifications;

    @Transactional(readOnly = true, transactionManager = "notificationTx")
    public List<NotificationView> listFor(String recipientId) {
        return notifications.findByRecipientIdOrderByCreatedAtDesc(recipientId).stream()
                .map(n -> new NotificationView(
                        n.getId(), n.getRecipientId(), n.getType(), n.getText(), n.isRead(), n.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}
