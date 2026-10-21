package com.lms.seed;

import com.lms.analytics.entity.CourseStatEntity;
import com.lms.analytics.repo.CourseStatRepository;
import com.lms.assessment.entity.AssignmentEntity;
import com.lms.assessment.entity.AttemptEntity;
import com.lms.assessment.entity.GradeEntity;
import com.lms.assessment.repo.AssignmentRepository;
import com.lms.assessment.repo.AttemptRepository;
import com.lms.assessment.repo.GradeRepository;
import com.lms.catalog.entity.CategoryEntity;
import com.lms.catalog.entity.CourseEntity;
import com.lms.catalog.entity.LessonEntity;
import com.lms.catalog.entity.ModuleEntity;
import com.lms.catalog.repo.CategoryRepository;
import com.lms.catalog.repo.CourseRepository;
import com.lms.catalog.repo.LessonRepository;
import com.lms.catalog.repo.ModuleRepository;
import com.lms.enrollment.entity.EnrollmentEntity;
import com.lms.enrollment.repo.EnrollmentRepository;
import com.lms.identity.entity.RoleEntity;
import com.lms.identity.entity.UserEntity;
import com.lms.identity.entity.UserRoleEntity;
import com.lms.identity.repo.RoleRepository;
import com.lms.identity.repo.UserRepository;
import com.lms.identity.repo.UserRoleRepository;
import com.lms.notification.entity.NotificationEntity;
import com.lms.notification.repo.NotificationRepository;
import com.lms.progress.entity.ProgressEntity;
import com.lms.progress.repo.ProgressRepository;
import com.lms.shared.Ids;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class DemoDataLoader implements ApplicationRunner {
    private final RoleRepository roles;
    private final UserRepository users;
    private final UserRoleRepository userRoles;
    private final CategoryRepository categories;
    private final CourseRepository courses;
    private final ModuleRepository modules;
    private final LessonRepository lessons;
    private final EnrollmentRepository enrollments;
    private final AssignmentRepository assignments;
    private final AttemptRepository attempts;
    private final GradeRepository grades;
    private final ProgressRepository progress;
    private final NotificationRepository notifications;
    private final CourseStatRepository stats;
    private final PlatformTransactionManager identityTx;
    private final PlatformTransactionManager catalogTx;
    private final PlatformTransactionManager enrollmentTx;
    private final PlatformTransactionManager assessmentTx;
    private final PlatformTransactionManager progressTx;
    private final PlatformTransactionManager notificationTx;
    private final PlatformTransactionManager analyticsTx;

    public DemoDataLoader(RoleRepository roles, UserRepository users, UserRoleRepository userRoles,
                          CategoryRepository categories,
                          CourseRepository courses, ModuleRepository modules, LessonRepository lessons,
                          EnrollmentRepository enrollments, AssignmentRepository assignments,
                          AttemptRepository attempts, GradeRepository grades, ProgressRepository progress,
                          NotificationRepository notifications, CourseStatRepository stats,
                          @Qualifier("identityTx") PlatformTransactionManager identityTx,
                          @Qualifier("catalogTx") PlatformTransactionManager catalogTx,
                          @Qualifier("enrollmentTx") PlatformTransactionManager enrollmentTx,
                          @Qualifier("assessmentTx") PlatformTransactionManager assessmentTx,
                          @Qualifier("progressTx") PlatformTransactionManager progressTx,
                          @Qualifier("notificationTx") PlatformTransactionManager notificationTx,
                          @Qualifier("analyticsTx") PlatformTransactionManager analyticsTx) {
        this.roles = roles;
        this.users = users;
        this.userRoles = userRoles;
        this.categories = categories;
        this.courses = courses;
        this.modules = modules;
        this.lessons = lessons;
        this.enrollments = enrollments;
        this.assignments = assignments;
        this.attempts = attempts;
        this.grades = grades;
        this.progress = progress;
        this.notifications = notifications;
        this.stats = stats;
        this.identityTx = identityTx;
        this.catalogTx = catalogTx;
        this.enrollmentTx = enrollmentTx;
        this.assessmentTx = assessmentTx;
        this.progressTx = progressTx;
        this.notificationTx = notificationTx;
        this.analyticsTx = analyticsTx;
    }

    @Override
    public void run(ApplicationArguments args) {
        new TransactionTemplate(identityTx).executeWithoutResult(s -> seedIdentity());
        new TransactionTemplate(catalogTx).executeWithoutResult(s -> seedCatalog());
        new TransactionTemplate(enrollmentTx).executeWithoutResult(s -> seedEnrollment());
        new TransactionTemplate(assessmentTx).executeWithoutResult(s -> seedAssessment());
        new TransactionTemplate(progressTx).executeWithoutResult(s -> seedProgress());
        new TransactionTemplate(notificationTx).executeWithoutResult(s -> seedNotifications());
        new TransactionTemplate(analyticsTx).executeWithoutResult(s -> seedAnalytics());
    }

    private void seedIdentity() {
        if (users.existsById(Ids.STUDENT)) {
            return;
        }
        RoleEntity student = roles.save(new RoleEntity("role-student", "student", "Студент"));
        RoleEntity teacher = roles.save(new RoleEntity("role-teacher", "teacher", "Преподаватель"));
        RoleEntity admin = roles.save(new RoleEntity("role-admin", "admin", "Администратор"));
        UserEntity u1 = users.save(new UserEntity(
                Ids.STUDENT, "student@lms.local", "stub", "Анна Студентова", "active", "2026-09-01T09:00:00"));
        UserEntity u2 = users.save(new UserEntity(
                Ids.TEACHER, "teacher@lms.local", "stub", "Павел Преподавателев", "active", "2026-08-15T09:00:00"));
        UserEntity u3 = users.save(new UserEntity(
                Ids.ADMIN, "admin@lms.local", "stub", "Ольга Админова", "active", "2026-08-01T09:00:00"));
        userRoles.save(new UserRoleEntity("ur-student", u1, student));
        userRoles.save(new UserRoleEntity("ur-teacher", u2, teacher));
        userRoles.save(new UserRoleEntity("ur-admin", u3, admin));
    }

    private void seedCatalog() {
        if (courses.existsById(Ids.COURSE)) {
            return;
        }
        CategoryEntity cat = categories.save(new CategoryEntity(
                Ids.CATEGORY, "Программирование", "Курсы по языкам и инструментам разработки"));
        CourseEntity course = courses.save(new CourseEntity(
                Ids.COURSE, Ids.TEACHER, "Основы Python", "beginner", "published", 4.2, cat));
        courses.save(new CourseEntity(
                Ids.DRAFT_COURSE, Ids.TEACHER, "Черновик курса Java", "beginner", "draft", 0, cat));
        ModuleEntity module = modules.save(new ModuleEntity(Ids.MODULE, "Синтаксис", 1, course));
        lessons.save(new LessonEntity(Ids.LESSON_1, "Переменные", "text", 1, module));
        lessons.save(new LessonEntity(Ids.LESSON_2, "Функции", "text", 2, module));
    }

    private void seedEnrollment() {
        if (enrollments.existsById(Ids.ENROLLMENT)) {
            return;
        }
        enrollments.save(new EnrollmentEntity(
                Ids.ENROLLMENT, Ids.STUDENT, Ids.COURSE, "2026-09-01T10:00:00", "active"
        ));
    }

    private void seedAssessment() {
        if (assignments.existsById(Ids.ASSIGNMENT)) {
            return;
        }
        AssignmentEntity assignment = assignments.save(new AssignmentEntity(
                Ids.ASSIGNMENT, Ids.LESSON_1, "Практика: переменные", "2026-12-01T23:59:00", 10
        ));
        AttemptEntity attempt = attempts.save(new AttemptEntity(
                Ids.ATTEMPT, Ids.STUDENT, "2026-09-10T12:00:00", "x = 1", "graded", assignment
        ));
        grades.save(new GradeEntity(
                Ids.GRADE, Ids.TEACHER, 8, "Хорошо, добавьте пояснение.", "2026-09-10T13:00:00", attempt
        ));
    }

    private void seedProgress() {
        if (progress.existsById(Ids.PROGRESS)) {
            return;
        }
        progress.save(new ProgressEntity(Ids.PROGRESS, Ids.ENROLLMENT, 50, "2026-09-10T12:30:00"));
    }

    private void seedNotifications() {
        if (notifications.existsById(Ids.NOTIF)) {
            return;
        }
        notifications.save(new NotificationEntity(
                Ids.NOTIF, Ids.STUDENT, "GradeAssigned",
                "Выставлена оценка 8 за «Практика: переменные».", false, "2026-09-10T13:00:00"
        ));
        notifications.save(new NotificationEntity(
                "16161616-1616-1616-1616-161616161616", Ids.STUDENT, "StudentEnrolled",
                "Вы записаны на курс «Основы Python».", true, "2026-09-01T10:01:00"
        ));
    }

    private void seedAnalytics() {
        if (stats.existsById(Ids.COURSE)) {
            return;
        }
        stats.save(new CourseStatEntity(Ids.COURSE, 1, 8.0, "2026-09-10T13:00:00"));
    }
}
