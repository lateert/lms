package com.lms.enrollment;

import com.lms.shared.event.LessonCompleted;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EnrollmentEventListener {
    private final EnrollmentService enrollment;

    @TransactionalEventListener(fallbackExecution = true)
    public void on(LessonCompleted event) {
        enrollment.handleLessonCompleted(event);
    }
}
