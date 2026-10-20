package com.lms.shared.view;

public record NotificationView(
        String id,
        String recipientId,
        String type,
        String text,
        boolean isRead,
        String createdAt
) {
}
