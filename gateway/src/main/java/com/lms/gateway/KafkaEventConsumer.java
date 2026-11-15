package com.lms.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.analytics.AnalyticsService;
import com.lms.catalog.CatalogService;
import com.lms.enrollment.EnrollmentService;
import com.lms.notification.NotificationService;
import com.lms.progress.ProgressService;
import com.lms.shared.event.AssignmentSubmitted;
import com.lms.shared.event.CoursePublished;
import com.lms.shared.event.CourseRatingUpdated;
import com.lms.shared.event.GradeAssigned;
import com.lms.shared.event.LessonCompleted;
import com.lms.shared.event.StudentEnrolled;
import com.lms.shared.event.UserRegistered;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventConsumer {
    private final ObjectMapper json;
    private final EventDrain drain;
    private final NotificationService notifications;
    private final AnalyticsService analytics;
    private final ProgressService progress;
    private final CatalogService catalog;
    private final EnrollmentService enrollment;

    @KafkaListener(topics = KafkaEventPublisher.TOPIC, groupId = "lms")
    public void onEvent(String payload) {
        KafkaEventPublisher.EventMessage message = null;
        try {
            message = json.readValue(payload, KafkaEventPublisher.EventMessage.class);
            log.info("Kafka {} получено, id {}", message.type(), message.id());
            switch (message.type()) {
                case "UserRegistered" -> notifications.handle(json.treeToValue(message.body(), UserRegistered.class));
                case "CoursePublished" -> {
                    CoursePublished event = json.treeToValue(message.body(), CoursePublished.class);
                    notifications.handle(event);
                    analytics.handle(event);
                }
                case "StudentEnrolled" -> {
                    StudentEnrolled event = json.treeToValue(message.body(), StudentEnrolled.class);
                    progress.handleStudentEnrolled(event);
                    notifications.handle(event);
                    analytics.handle(event);
                }
                case "LessonCompleted" -> {
                    LessonCompleted event = json.treeToValue(message.body(), LessonCompleted.class);
                    notifications.handle(event);
                    enrollment.handleLessonCompleted(event);
                }
                case "AssignmentSubmitted" ->
                        notifications.handle(json.treeToValue(message.body(), AssignmentSubmitted.class));
                case "GradeAssigned" -> {
                    GradeAssigned event = json.treeToValue(message.body(), GradeAssigned.class);
                    progress.handleGradeAssigned(event);
                    notifications.handle(event);
                    analytics.handle(event);
                }
                case "CourseRatingUpdated" ->
                        catalog.handleRatingUpdated(json.treeToValue(message.body(), CourseRatingUpdated.class));
                default -> log.warn("Неизвестное событие {}", message.type());
            }
            log.info("Kafka {} обработано, id {}", message.type(), message.id());
        } catch (Exception ex) {
            String type = message == null ? "не разобрано" : message.type();
            String id = message == null ? "-" : message.id();
            log.error("Kafka {} не обработано, id {}. Сообщение пропущено, остальные события читаются дальше", type, id, ex);
        } finally {
            if (message != null && message.id() != null) {
                drain.finished(message.id());
            }
        }
    }
}
