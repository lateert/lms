package com.lms.gateway;

import com.lms.analytics.AnalyticsService;
import com.lms.assessment.AssessmentService;
import com.lms.catalog.CatalogService;
import com.lms.enrollment.EnrollmentService;
import com.lms.shared.view.AssignmentView;
import com.lms.shared.view.AttemptView;
import com.lms.shared.view.CatalogFilterInput;
import com.lms.shared.view.CourseAnalyticsView;
import com.lms.shared.view.CourseView;
import com.lms.shared.view.EnrollmentView;
import com.lms.shared.view.NotificationView;
import com.lms.shared.view.ProgressView;
import com.lms.shared.view.UserView;
import com.lms.notification.NotificationService;
import com.lms.progress.ProgressService;
import com.lms.shared.AppException;
import com.lms.shared.UserSnapshot;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class QueryController {
    private final CatalogService catalog;
    private final EnrollmentService enrollment;
    private final AssessmentService assessment;
    private final ProgressService progress;
    private final NotificationService notification;
    private final AnalyticsService analytics;

    @QueryMapping
    public List<CourseView> catalog(@Argument CatalogFilterInput filter) {
        String categoryId = filter == null ? null : filter.categoryId();
        String difficulty = filter == null ? null : filter.difficulty();
        String sortBy = filter == null ? null : filter.sortBy();
        return catalog.listPublished(categoryId, difficulty, sortBy);
    }

    @QueryMapping
    public CourseView course(@Argument String id) {
        return catalog.getCourseTree(id);
    }

    @QueryMapping
    public UserView me(@ContextValue(name = "user", required = false) UserSnapshot user) {
        return UserView.from(requireUser(user));
    }

    @QueryMapping
    public List<EnrollmentView> myEnrollments(@ContextValue(name = "user", required = false) UserSnapshot user) {
        return enrollment.listForStudent(requireUser(user).id());
    }

    @QueryMapping
    public ProgressView myProgress(@Argument String courseId,
                                   @ContextValue(name = "user", required = false) UserSnapshot user) {
        return progress.getForCourse(requireUser(user).id(), courseId);
    }

    @QueryMapping
    public List<NotificationView> myNotifications(@ContextValue(name = "user", required = false) UserSnapshot user) {
        return notification.listFor(requireUser(user).id());
    }

    @QueryMapping
    public List<AttemptView> attemptJournal(@Argument String courseId,
                                            @ContextValue(name = "user", required = false) UserSnapshot user) {
        return assessment.attemptJournal(requireUser(user).id(), courseId);
    }

    @QueryMapping
    public CourseAnalyticsView courseAnalytics(@Argument String courseId) {
        return analytics.courseAnalytics(courseId);
    }

    @QueryMapping
    public List<EnrollmentView> learningHistory(@ContextValue(name = "user", required = false) UserSnapshot user) {
        return enrollment.listForStudent(requireUser(user).id());
    }

    @SchemaMapping(typeName = "Course", field = "assignments")
    public List<AssignmentView> assignments(CourseView course) {
        return assessment.listAssignments(course.id());
    }

    @SchemaMapping(typeName = "Enrollment", field = "progress")
    public ProgressView progress(EnrollmentView enrollment) {
        return this.progress.getByEnrollmentId(enrollment.id());
    }

    private UserSnapshot requireUser(UserSnapshot user) {
        if (user == null) {
            throw AppException.unauthorized("Сессия недействительна");
        }
        return user;
    }
}
