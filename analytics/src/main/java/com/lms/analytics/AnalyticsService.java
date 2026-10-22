package com.lms.analytics;

import com.lms.analytics.entity.CourseStatEntity;
import com.lms.analytics.repo.CourseStatRepository;
import com.lms.shared.AppException;
import com.lms.shared.EventPublisher;
import com.lms.shared.Times;
import com.lms.shared.event.CoursePublished;
import com.lms.shared.event.CourseRatingUpdated;
import com.lms.shared.event.GradeAssigned;
import com.lms.shared.event.StudentEnrolled;
import com.lms.shared.view.CourseAnalyticsView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final CourseStatRepository stats;
    private final EventPublisher events;

    @Transactional(readOnly = true, transactionManager = "analyticsTx")
    public CourseAnalyticsView courseAnalytics(String courseId) {
        CourseStatEntity row = stats.findById(courseId)
                .orElseThrow(() -> AppException.notFound("Аналитика по курсу не найдена"));
        return new CourseAnalyticsView(
                row.getCourseId(), row.getEnrollCount(), row.getAvgScore(), row.getGradeCount(), row.getUpdatedAt()
        );
    }

    @Transactional(transactionManager = "analyticsTx", propagation = Propagation.REQUIRES_NEW)
    public void handle(CoursePublished event) {
        rowFor(event.courseId());
    }

    @Transactional(transactionManager = "analyticsTx", propagation = Propagation.REQUIRES_NEW)
    public void handle(StudentEnrolled event) {
        CourseStatEntity row = rowFor(event.courseId());
        row.setEnrollCount(row.getEnrollCount() + 1);
        row.setUpdatedAt(Times.now());
    }

    @Transactional(transactionManager = "analyticsTx", propagation = Propagation.REQUIRES_NEW)
    public void handle(GradeAssigned event) {
        CourseStatEntity row = rowFor(event.courseId());
        double percent = event.maxScore() > 0 ? event.score() * 100 / event.maxScore() : 0;
        int count = row.getGradeCount();
        double average = (row.getAvgScore() * count + percent) / (count + 1);
        row.setGradeCount(count + 1);
        row.setAvgScore(Math.round(average * 100) / 100.0);
        row.setUpdatedAt(Times.now());
        events.publish(new CourseRatingUpdated(event.courseId(), Math.round(row.getAvgScore() / 2) / 10.0));
    }

    private CourseStatEntity rowFor(String courseId) {
        return stats.findById(courseId)
                .orElseGet(() -> stats.save(new CourseStatEntity(courseId, 0, 0, 0, Times.now())));
    }
}
