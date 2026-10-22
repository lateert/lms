package com.lms;

import com.lms.shared.Ids;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GraphqlMutationTest {

    @Autowired
    private TestRestTemplate rest;

    @SuppressWarnings("unchecked")
    private Map<String, Object> gql(String token, String query, Map<String, Object> variables) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        Map<String, Object> body = Map.of("query", query, "variables", variables);
        ResponseEntity<Map> response = rest.postForEntity("/graphql", new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> ok(String token, String query, Map<String, Object> variables) {
        Map<String, Object> body = gql(token, query, variables);
        assertThat(body.get("errors")).as(String.valueOf(body.get("errors"))).isNull();
        return (Map<String, Object>) body.get("data");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> field(Map<String, Object> data, String name) {
        return (Map<String, Object>) data.get(name);
    }

    @SuppressWarnings("unchecked")
    private void fails(String token, String query, Map<String, Object> variables, String code, String message) {
        List<Map<String, Object>> errors = (List<Map<String, Object>>) gql(token, query, variables).get("errors");
        assertThat(errors).isNotEmpty();
        Map<String, Object> extensions = (Map<String, Object>) errors.get(0).get("extensions");
        assertThat(extensions.get("code")).isEqualTo(code);
        assertThat((String) errors.get(0).get("message")).contains(message);
    }

    private String login(String email) {
        Map<String, Object> data = ok(null,
                "mutation($in: LoginInput!) { login(input: $in) { token } }",
                Map.of("in", Map.of("email", email, "password", Ids.DEMO_PASSWORD)));
        return (String) field(data, "login").get("token");
    }

    @Test
    void courseLifecycle() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String teacher = login("teacher@lms.local");

        fails(null, "mutation($in: LoginInput!) { login(input: $in) { token } }",
                Map.of("in", Map.of("email", "teacher@lms.local", "password", "wrong")),
                "UNAUTHORIZED", "Неверный");

        String courseId = (String) field(ok(teacher,
                "mutation($in: CreateCourseInput!) { createCourse(input: $in) { id status } }",
                Map.of("in", Map.of("categoryId", Ids.CATEGORY, "title", "Spring " + suffix,
                        "difficulty", "intermediate"))), "createCourse").get("id");

        String publish = "mutation($id: ID!) { publishCourse(courseId: $id) { status } }";
        fails(teacher, publish, Map.of("id", courseId), "CONFLICT", "без модулей");

        String moduleId = (String) field(ok(teacher,
                "mutation($in: AddModuleInput!) { addModule(input: $in) { id sortOrder } }",
                Map.of("in", Map.of("courseId", courseId, "title", "Введение"))), "addModule").get("id");
        fails(teacher, publish, Map.of("id", courseId), "CONFLICT", "нет уроков");

        String addLesson = "mutation($in: AddLessonInput!) { addLesson(input: $in) { id } }";
        String lesson1 = (String) field(ok(teacher, addLesson, Map.of("in",
                Map.of("moduleId", moduleId, "title", "Бины", "contentType", "text"))), "addLesson").get("id");
        ok(teacher, addLesson, Map.of("in",
                Map.of("moduleId", moduleId, "title", "Контекст", "contentType", "video")));

        String assignmentId = (String) field(ok(teacher,
                "mutation($in: AddAssignmentInput!) { addAssignment(input: $in) { id } }",
                Map.of("in", Map.of("lessonId", lesson1, "title", "ДЗ " + suffix,
                        "deadline", "2030-01-01T23:59:00", "maxScore", 10))), "addAssignment").get("id");

        String enroll = "mutation($id: ID!) { enrollCourse(courseId: $id) { status } }";
        Map<String, Object> reg = field(ok(null,
                "mutation($in: RegisterInput!) { register(input: $in) { token user { id roles } } }",
                Map.of("in", Map.of("email", "s" + suffix + "@lms.local", "password", "secret1",
                        "fullName", "Тест Студент"))), "register");
        String student = (String) reg.get("token");
        String studentId = (String) field(reg, "user").get("id");

        fails(student, enroll, Map.of("id", courseId), "CONFLICT", "не опубликован");
        assertThat(field(ok(teacher, publish, Map.of("id", courseId)), "publishCourse").get("status"))
                .isEqualTo("published");
        fails(teacher, publish, Map.of("id", courseId), "CONFLICT", "уже опубликован");

        fails(teacher, enroll, Map.of("id", courseId), "FORBIDDEN", "");
        assertThat(field(ok(student, enroll, Map.of("id", courseId)), "enrollCourse").get("status"))
                .isEqualTo("active");
        fails(student, enroll, Map.of("id", courseId), "CONFLICT", "уже записан");

        String complete = "mutation($id: ID!) { completeLesson(lessonId: $id) { percent } }";
        assertThat(((Number) field(ok(student, complete, Map.of("id", lesson1)), "completeLesson")
                .get("percent")).doubleValue()).isEqualTo(50.0);
        fails(student, complete, Map.of("id", lesson1), "CONFLICT", "уже отмечен");

        String submit = "mutation($in: SubmitAttemptInput!) { submitAttempt(input: $in) { id status } }";
        fails(student, submit, Map.of("in", Map.of("assignmentId", assignmentId, "content", " ")),
                "BAD_REQUEST", "");
        String attemptId = (String) field(ok(student, submit,
                Map.of("in", Map.of("assignmentId", assignmentId, "content", "@Component"))),
                "submitAttempt").get("id");
        fails(student, submit, Map.of("in", Map.of("assignmentId", assignmentId, "content", "ещё")),
                "CONFLICT", "не проверена");

        String grade = "mutation($in: GradeAttemptInput!) { gradeAttempt(input: $in) { status grade { score } } }";
        fails(student, grade, Map.of("in", Map.of("attemptId", attemptId, "score", 9)), "FORBIDDEN", "");
        fails(teacher, grade, Map.of("in", Map.of("attemptId", attemptId, "score", 11)), "BAD_REQUEST", "");
        assertThat(field(ok(teacher, grade,
                Map.of("in", Map.of("attemptId", attemptId, "score", 9, "comment", "Отлично"))),
                "gradeAttempt").get("status")).isEqualTo("graded");
        fails(teacher, grade, Map.of("in", Map.of("attemptId", attemptId, "score", 7)), "CONFLICT", "уже оценена");

        String notifications = ok(student, "{ myNotifications(unreadOnly: true) { type text } }", Map.of()).toString();
        assertThat(notifications).contains("UserRegistered", "StudentEnrolled", "GradeAssigned", "9");
        assertThat(ok(teacher, "{ myNotifications { type } }", Map.of()).toString())
                .contains("AssignmentSubmitted");

        Map<String, Object> analytics = field(ok(teacher,
                "query($id: ID!) { courseAnalytics(courseId: $id) { enrollCount avgScore gradeCount } }",
                Map.of("id", courseId)), "courseAnalytics");
        assertThat(((Number) analytics.get("enrollCount")).intValue()).isEqualTo(1);
        assertThat(((Number) analytics.get("avgScore")).doubleValue()).isEqualTo(90.0);
        assertThat(((Number) field(ok(teacher, "query($id: ID!) { course(id: $id) { rating } }",
                Map.of("id", courseId)), "course").get("rating")).doubleValue()).isEqualTo(4.5);

        Map<String, Object> summary = field(ok(teacher,
                "query($id: ID!) { attemptSummary(courseId: $id) { total pending graded averagePercent } }",
                Map.of("id", courseId)), "attemptSummary");
        assertThat(((Number) summary.get("total")).intValue()).isEqualTo(1);
        assertThat(((Number) summary.get("graded")).intValue()).isEqualTo(1);
        assertThat(((Number) summary.get("averagePercent")).doubleValue()).isEqualTo(90.0);

        assertThat(ok(teacher,
                "query($id: ID!, $s: ID) { attemptJournal(courseId: $id, filter: {status: \"graded\", studentId: $s}) { studentId } }",
                Map.of("id", courseId, "s", studentId)).toString()).contains(studentId);
        fails(student, "query($id: ID!) { attemptJournal(courseId: $id) { id } }",
                Map.of("id", Ids.COURSE), "FORBIDDEN", "");

        assertThat(field(ok(student, "mutation($id: ID!) { dropCourse(courseId: $id) { status } }",
                Map.of("id", courseId)), "dropCourse").get("status")).isEqualTo("dropped");
    }

    @Test
    void catalogFiltersAndAggregation() {
        Map<String, Object> data = ok(null,
                "{ catalog(filter: {difficulty: \"advanced\"}) { title } }", Map.of());
        assertThat(data.toString()).contains("Алгоритмы").doesNotContain("SQL для аналитиков");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byRating = (List<Map<String, Object>>) ok(null,
                "{ catalog(sort: {field: RATING, direction: DESC}) { title rating } }", Map.of()).get("catalog");
        assertThat(byRating.get(0).get("title")).isEqualTo("SQL для аналитиков");

        assertThat(ok(null, "{ catalog(filter: {search: \"pyth\"}) { title } }", Map.of()).toString())
                .contains("Основы Python");

        Map<String, Object> summary = field(ok(null,
                "query($c: ID) { catalogSummary(filter: {categoryId: $c}) { total avgRating byDifficulty { difficulty count } } }",
                Map.of("c", Ids.CATEGORY_DB)), "catalogSummary");
        assertThat(((Number) summary.get("total")).intValue()).isEqualTo(1);
        assertThat(((Number) summary.get("avgRating")).doubleValue()).isEqualTo(4.6);

        fails(null, "{ catalog(filter: {difficulty: \"expert\"}) { id } }", Map.of(), "BAD_REQUEST", "");
        fails(null, "{ catalog(filter: {minRating: 7}) { id } }", Map.of(), "BAD_REQUEST", "");
    }

    @Test
    void roleAssignmentRequiresAdmin() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String userId = (String) field(field(ok(null,
                "mutation($in: RegisterInput!) { register(input: $in) { user { id } } }",
                Map.of("in", Map.of("email", "t" + suffix + "@lms.local", "password", "secret1",
                        "fullName", "Новый Преподаватель"))), "register"), "user").get("id");
        String assign = "mutation($in: AssignRoleInput!) { assignRole(input: $in) { roles } }";
        Map<String, Object> input = Map.of("in", Map.of("userId", userId, "roleCode", "teacher"));

        fails(login("teacher@lms.local"), assign, input, "FORBIDDEN", "");
        String admin = login("admin@lms.local");
        assertThat(ok(admin, assign, input).toString()).contains("teacher");
        fails(admin, assign, input, "CONFLICT", "");

        fails(null, "mutation($in: RegisterInput!) { register(input: $in) { token } }",
                Map.of("in", Map.of("email", "t" + suffix + "@lms.local", "password", "secret1",
                        "fullName", "Дубль")), "CONFLICT", "");
        fails(null, "mutation($in: RegisterInput!) { register(input: $in) { token } }",
                Map.of("in", Map.of("email", "bad-email", "password", "1", "fullName", "X")), "BAD_REQUEST", "");
    }
}
