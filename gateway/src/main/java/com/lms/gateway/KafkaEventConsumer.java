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
        try {
            KafkaEventPublisher.EventMessage message = json.readValue(payload, KafkaEventPublisher.EventMessage.class);
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
        } catch (Exception ex) {
            log.error("Не удалось обработать событие Kafka", ex);
        } finally {
            try {
                KafkaEventPublisher.EventMessage message = json.readValue(payload, KafkaEventPublisher.EventMessage.class);
                drain.finished(message.id());
            } catch (Exception ex) {
                log.error("Событие Kafka без идентификатора", ex);
            }
        }
    }
}
