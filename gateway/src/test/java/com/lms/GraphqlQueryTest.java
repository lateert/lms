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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GraphqlQueryTest {

    @Autowired
    private TestRestTemplate rest;

    private Map<String, Object> gql(String query) {
        return gql(query, Map.of(), null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> gql(String query, Map<String, Object> variables, String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (userId != null) {
            headers.set("X-User-Id", userId);
        }
        Map<String, Object> body = Map.of("query", query, "variables", variables);
        ResponseEntity<Map> response = rest.postForEntity("/graphql", new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    @Test
    void catalogAndCourse() {
        Map<String, Object> data = data(gql("{ catalog { id title status category { name } } }"));
        assertThat(data.get("catalog").toString()).contains("Основы Python");

        Map<String, Object> course = data(gql(
                "query($id: ID!) { course(id: $id) { title modules { lessons { title } } assignments { title maxScore } } }",
                Map.of("id", Ids.COURSE),
                null
        ));
        assertThat(course.get("course").toString()).contains("Практика: переменные");

        Map<String, Object> missing = data(gql("{ course(id: \"00000000-0000-0000-0000-000000000000\") { id } }"));
        assertThat(missing.get("course")).isNull();
    }

    @Test
    void studentQueries() {
        assertThat(data(gql("{ me { email fullName roles } }")).get("me").toString())
                .contains("student@lms.local");
        assertThat(data(gql("{ myEnrollments { status course { title } } }")).toString())
                .contains("Основы Python");
        assertThat(data(gql(
                "query($id: ID!) { myProgress(courseId: $id) { percent } }",
                Map.of("id", Ids.COURSE),
                null
        )).toString()).contains("50");
        assertThat(data(gql("{ myNotifications { type } }")).toString()).contains("GradeAssigned");
        assertThat(data(gql(
                "query($id: ID!) { attemptJournal(courseId: $id) { grade { score } } }",
                Map.of("id", Ids.COURSE),
                null
        )).toString()).contains("8");
        assertThat(data(gql(
                "query($id: ID!) { courseAnalytics(courseId: $id) { enrollCount } }",
                Map.of("id", Ids.COURSE),
                null
        )).toString()).contains("1");
    }

    @Test
    void errors() {
        Map<String, Object> badUser = gql("{ me { email } }", Map.of(), "not-a-user");
        assertThat(badUser.get("errors")).isNotNull();

        Map<String, Object> noProgress = gql(
                "query($id: ID!) { myProgress(courseId: $id) { percent } }",
                Map.of("id", Ids.DRAFT_COURSE),
                null
        );
        assertThat(noProgress.get("errors")).isNotNull();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> body) {
        assertThat(body.get("errors")).isNull();
        return (Map<String, Object>) body.get("data");
    }
}
