package com.lms.gateway;

import com.lms.analytics.AnalyticsService;
import com.lms.assessment.AssessmentService;
import com.lms.catalog.CatalogService;
import com.lms.enrollment.EnrollmentService;
import com.lms.notification.NotificationService;
import com.lms.progress.ProgressService;
import com.lms.shared.UserSnapshot;
import com.lms.shared.input.AttemptFilterInput;
import com.lms.shared.input.CatalogFilterInput;
import com.lms.shared.input.CourseSortInput;
import com.lms.shared.input.SortDirection;
import com.lms.shared.view.AssignmentView;
import com.lms.shared.view.AttemptSummaryView;
import com.lms.shared.view.AttemptView;
import com.lms.shared.view.CatalogSummaryView;
import com.lms.shared.view.CourseAnalyticsView;
import com.lms.shared.view.CourseView;
import com.lms.shared.view.EnrollmentView;
import com.lms.shared.view.NotificationView;
import com.lms.shared.view.ProgressView;
import com.lms.shared.view.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
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
    public List<CourseView> catalog(@Argument CatalogFilterInput filter, @Argument CourseSortInput sort) {
        return catalog.listPublished(filter, sort);
    }

    @QueryMapping
    public CatalogSummaryView catalogSummary(@Argument CatalogFilterInput filter) {
        return catalog.summary(filter);
    }

    @QueryMapping
    public CourseView course(@Argument String id) {
        return catalog.getCourseTree(id);
    }

    @QueryMapping
    public UserView me(@ContextValue(name = "user", required = false) UserSnapshot user) {
        return UserView.from(Actors.require(user));
    }

    @QueryMapping
    public List<EnrollmentView> myEnrollments(@Argument String status,
                                              @ContextValue(name = "user", required = false) UserSnapshot user) {
        return enrollment.listForStudent(Actors.require(user).id(), status);
    }

    @QueryMapping
    public ProgressView myProgress(@Argument String courseId,
                                   @ContextValue(name = "user", required = false) UserSnapshot user) {
        return progress.getForCourse(Actors.require(user).id(), courseId);
    }

    @QueryMapping
    public List<NotificationView> myNotifications(@Argument Boolean unreadOnly,
                                                  @ContextValue(name = "user", required = false) UserSnapshot user) {
        return notification.listFor(Actors.require(user).id(), Boolean.TRUE.equals(unreadOnly));
    }

    @QueryMapping
    public List<AttemptView> attemptJournal(@Argument String courseId,
                                            @Argument AttemptFilterInput filter,
                                            @Argument SortDirection order,
                                            @ContextValue(name = "user", required = false) UserSnapshot user) {
        return assessment.attemptJournal(Actors.require(user), courseId, filter, order);
    }

    @QueryMapping
    public AttemptSummaryView attemptSummary(@Argument String courseId,
                                             @ContextValue(name = "user", required = false) UserSnapshot user) {
        return assessment.attemptSummary(Actors.require(user), courseId);
    }

    @QueryMapping
    public CourseAnalyticsView courseAnalytics(@Argument String courseId) {
        return analytics.courseAnalytics(courseId);
    }

    @QueryMapping
    public List<EnrollmentView> learningHistory(@ContextValue(name = "user", required = false) UserSnapshot user) {
        return enrollment.listForStudent(Actors.require(user).id(), null);
    }

    @SchemaMapping(typeName = "Course", field = "assignments")
    public List<AssignmentView> assignments(CourseView course) {
        return assessment.listAssignments(course.id());
    }

    @SchemaMapping(typeName = "Enrollment", field = "progress")
    public ProgressView progress(EnrollmentView enrollment) {
        return progress.findByEnrollmentId(enrollment.id());
    }
}
