package com.lms.analytics;

import com.lms.analytics.entity.CourseStatEntity;
import com.lms.analytics.repo.CourseStatRepository;
import com.lms.shared.AppException;
import com.lms.shared.view.CourseAnalyticsView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final CourseStatRepository stats;

    @Transactional(readOnly = true, transactionManager = "analyticsTx")
    public CourseAnalyticsView courseAnalytics(String courseId) {
        CourseStatEntity row = stats.findById(courseId)
                .orElseThrow(() -> AppException.notFound("Аналитика по курсу не найдена"));
        return new CourseAnalyticsView(row.getCourseId(), row.getEnrollCount(), row.getAvgScore(), row.getUpdatedAt());
    }
}
