package com.lms.analytics;

import com.lms.shared.event.CoursePublished;
import com.lms.shared.event.GradeAssigned;
import com.lms.shared.event.StudentEnrolled;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AnalyticsEventListener {
    private final AnalyticsService analytics;

    @TransactionalEventListener(fallbackExecution = true)
    public void on(CoursePublished event) {
        analytics.handle(event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(StudentEnrolled event) {
        analytics.handle(event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(GradeAssigned event) {
        analytics.handle(event);
    }
}
