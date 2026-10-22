package com.lms.progress;

import com.lms.shared.event.GradeAssigned;
import com.lms.shared.event.StudentEnrolled;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ProgressEventListener {
    private final ProgressService progress;

    @TransactionalEventListener(fallbackExecution = true)
    public void on(StudentEnrolled event) {
        progress.handleStudentEnrolled(event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(GradeAssigned event) {
        progress.handleGradeAssigned(event);
    }
}
