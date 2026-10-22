package com.lms.notification;

import com.lms.notification.entity.NotificationEntity;
import com.lms.notification.repo.NotificationRepository;
import com.lms.shared.AppException;
import com.lms.shared.Times;
import com.lms.shared.UserSnapshot;
import com.lms.shared.event.AssignmentSubmitted;
import com.lms.shared.event.CoursePublished;
import com.lms.shared.event.GradeAssigned;
import com.lms.shared.event.LessonCompleted;
import com.lms.shared.event.StudentEnrolled;
import com.lms.shared.event.UserRegistered;
import com.lms.shared.view.NotificationView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notifications;

    @Transactional(readOnly = true, transactionManager = "notificationTx")
    public List<NotificationView> listFor(String recipientId, boolean unreadOnly) {
        List<NotificationEntity> rows = unreadOnly
                ? notifications.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientId)
                : notifications.findByRecipientIdOrderByCreatedAtDesc(recipientId);
        return rows.stream().map(this::toView).collect(Collectors.toList());
    }

    @Transactional(transactionManager = "notificationTx")
    public NotificationView markRead(UserSnapshot actor, String notificationId) {
        NotificationEntity row = notifications.findById(notificationId)
                .orElseThrow(() -> AppException.notFound("Уведомление не найдено"));
        if (!row.getRecipientId().equals(actor.id())) {
            throw AppException.forbidden("Уведомление адресовано другому пользователю");
        }
        row.setRead(true);
        return toView(row);
    }

    @Transactional(transactionManager = "notificationTx", propagation = Propagation.REQUIRES_NEW)
    public void handle(UserRegistered event) {
        create(event.userId(), "UserRegistered", "Добро пожаловать в LMS, " + event.fullName() + "!");
    }

    @Transactional(transactionManager = "notificationTx", propagation = Propagation.REQUIRES_NEW)
    public void handle(CoursePublished event) {
        create(event.authorId(), "CoursePublished", "Курс «" + event.title() + "» опубликован в каталоге.");
    }

    @Transactional(transactionManager = "notificationTx", propagation = Propagation.REQUIRES_NEW)
    public void handle(StudentEnrolled event) {
        create(event.studentId(), "StudentEnrolled", "Вы записаны на курс «" + event.courseTitle() + "».");
    }

    @Transactional(transactionManager = "notificationTx", propagation = Propagation.REQUIRES_NEW)
    public void handle(LessonCompleted event) {
        if (event.percent() >= 100) {
            create(event.studentId(), "CourseCompleted", "Все уроки курса пройдены. Поздравляем!");
        }
    }

    @Transactional(transactionManager = "notificationTx", propagation = Propagation.REQUIRES_NEW)
    public void handle(AssignmentSubmitted event) {
        create(event.teacherId(), "AssignmentSubmitted",
                "Новая работа по заданию «" + event.assignmentTitle() + "» ждёт проверки.");
    }

    @Transactional(transactionManager = "notificationTx", propagation = Propagation.REQUIRES_NEW)
    public void handle(GradeAssigned event) {
        create(event.studentId(), "GradeAssigned",
                "Выставлена оценка " + formatScore(event.score()) + " из " + formatScore(event.maxScore())
                        + " за «" + event.assignmentTitle() + "».");
    }

    private void create(String recipientId, String type, String text) {
        notifications.save(new NotificationEntity(UUID.randomUUID().toString(), recipientId, type, text, false, Times.now()));
    }

    private static String formatScore(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private NotificationView toView(NotificationEntity n) {
        return new NotificationView(n.getId(), n.getRecipientId(), n.getType(), n.getText(), n.isRead(), n.getCreatedAt());
    }
}
