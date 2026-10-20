package com.lms.progress;

import com.lms.progress.entity.ProgressEntity;
import com.lms.progress.repo.ProgressRepository;
import com.lms.shared.AppException;
import com.lms.shared.EnrollmentPort;
import com.lms.shared.EnrollmentSnapshot;
import com.lms.shared.view.ProgressView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProgressService {
    private final ProgressRepository progress;
    private final EnrollmentPort enrollment;

    @Transactional(readOnly = true, transactionManager = "progressTx")
    public ProgressView getForCourse(String studentId, String courseId) {
        EnrollmentSnapshot enr = enrollment.find(studentId, courseId);
        if (enr == null) {
            throw AppException.notFound("Прогресс по курсу не найден");
        }
        return getByEnrollmentId(enr.id());
    }

    @Transactional(readOnly = true, transactionManager = "progressTx")
    public ProgressView getByEnrollmentId(String enrollmentId) {
        ProgressEntity row = progress.findByEnrollmentId(enrollmentId)
                .orElseThrow(() -> AppException.notFound("Прогресс по курсу не найден"));
        return new ProgressView(row.getId(), row.getEnrollmentId(), row.getPercent(), row.getUpdatedAt());
    }
}
