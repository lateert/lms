package com.lms.notification;

import com.lms.shared.event.AssignmentSubmitted;
import com.lms.shared.event.CoursePublished;
import com.lms.shared.event.GradeAssigned;
import com.lms.shared.event.LessonCompleted;
import com.lms.shared.event.StudentEnrolled;
import com.lms.shared.event.UserRegistered;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationService notifications;

    @TransactionalEventListener(fallbackExecution = true)
    public void on(UserRegistered event) {
        notifications.handle(event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(CoursePublished event) {
        notifications.handle(event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(StudentEnrolled event) {
        notifications.handle(event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(LessonCompleted event) {
        notifications.handle(event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(AssignmentSubmitted event) {
        notifications.handle(event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(GradeAssigned event) {
        notifications.handle(event);
    }
}
