package com.lms.gateway;

import com.lms.assessment.AssessmentService;
import com.lms.catalog.CatalogService;
import com.lms.enrollment.EnrollmentService;
import com.lms.identity.IdentityService;
import com.lms.notification.NotificationService;
import com.lms.progress.ProgressService;
import com.lms.shared.UserSnapshot;
import com.lms.shared.input.AddAssignmentInput;
import com.lms.shared.input.AddLessonInput;
import com.lms.shared.input.AddModuleInput;
import com.lms.shared.input.AssignRoleInput;
import com.lms.shared.input.CreateCourseInput;
import com.lms.shared.input.GradeAttemptInput;
import com.lms.shared.input.LoginInput;
import com.lms.shared.input.RegisterInput;
import com.lms.shared.input.SubmitAttemptInput;
import com.lms.shared.view.AssignmentView;
import com.lms.shared.view.AttemptView;
import com.lms.shared.view.AuthPayloadView;
import com.lms.shared.view.CourseView;
import com.lms.shared.view.EnrollmentView;
import com.lms.shared.view.LessonView;
import com.lms.shared.view.ModuleView;
import com.lms.shared.view.NotificationView;
import com.lms.shared.view.ProgressView;
import com.lms.shared.view.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MutationController {
    private final IdentityService identity;
    private final CatalogService catalog;
    private final EnrollmentService enrollment;
    private final ProgressService progress;
    private final AssessmentService assessment;
    private final NotificationService notification;

    @MutationMapping
    public AuthPayloadView register(@Argument RegisterInput input) {
        return identity.register(input);
    }

    @MutationMapping
    public AuthPayloadView login(@Argument LoginInput input) {
        return identity.login(input);
    }

    @MutationMapping
    public UserView assignRole(@Argument AssignRoleInput input,
                               @ContextValue(name = "user", required = false) UserSnapshot user) {
        return identity.assignRole(Actors.require(user), input);
    }

    @MutationMapping
    public CourseView createCourse(@Argument CreateCourseInput input,
                                   @ContextValue(name = "user", required = false) UserSnapshot user) {
        return catalog.createCourse(Actors.require(user), input);
    }

    @MutationMapping
    public ModuleView addModule(@Argument AddModuleInput input,
                                @ContextValue(name = "user", required = false) UserSnapshot user) {
        return catalog.addModule(Actors.require(user), input);
    }

    @MutationMapping
    public LessonView addLesson(@Argument AddLessonInput input,
                                @ContextValue(name = "user", required = false) UserSnapshot user) {
        return catalog.addLesson(Actors.require(user), input);
    }

    @MutationMapping
    public CourseView publishCourse(@Argument String courseId,
                                    @ContextValue(name = "user", required = false) UserSnapshot user) {
        return catalog.publishCourse(Actors.require(user), courseId);
    }

    @MutationMapping
    public AssignmentView addAssignment(@Argument AddAssignmentInput input,
                                        @ContextValue(name = "user", required = false) UserSnapshot user) {
        return assessment.addAssignment(Actors.require(user), input);
    }

    @MutationMapping
    public EnrollmentView enrollCourse(@Argument String courseId,
                                       @ContextValue(name = "user", required = false) UserSnapshot user) {
        return enrollment.enroll(Actors.require(user), courseId);
    }

    @MutationMapping
    public EnrollmentView dropCourse(@Argument String courseId,
                                     @ContextValue(name = "user", required = false) UserSnapshot user) {
        return enrollment.drop(Actors.require(user), courseId);
    }

    @MutationMapping
    public ProgressView completeLesson(@Argument String lessonId,
                                       @ContextValue(name = "user", required = false) UserSnapshot user) {
        return progress.completeLesson(Actors.require(user), lessonId);
    }

    @MutationMapping
    public AttemptView submitAttempt(@Argument SubmitAttemptInput input,
                                     @ContextValue(name = "user", required = false) UserSnapshot user) {
        return assessment.submitAttempt(Actors.require(user), input);
    }

    @MutationMapping
    public AttemptView gradeAttempt(@Argument GradeAttemptInput input,
                                    @ContextValue(name = "user", required = false) UserSnapshot user) {
        return assessment.gradeAttempt(Actors.require(user), input);
    }

    @MutationMapping
    public NotificationView markNotificationRead(@Argument String id,
                                                 @ContextValue(name = "user", required = false) UserSnapshot user) {
        return notification.markRead(Actors.require(user), id);
    }
}
