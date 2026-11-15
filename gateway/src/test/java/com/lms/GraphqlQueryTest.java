package com.lms;

import com.lms.catalog.CatalogCache;
import com.lms.shared.Ids;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    @Autowired
    private StringRedisTemplate redis;

    private Map<String, Object> gql(String query) {
        return gql(query, Map.of(), null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> gql(String query, Map<String, Object> variables, String token) {
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
    private String studentToken() {
        Map<String, Object> body = data(gql(
                "mutation($in: LoginInput!) { login(input: $in) { token } }",
                Map.of("in", Map.of("email", "student@lms.local", "password", Ids.DEMO_PASSWORD)),
                null
        ));
        return (String) ((Map<String, Object>) body.get("login")).get("token");
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
        String token = studentToken();
        assertThat(data(gql("{ me { email fullName roles } }", Map.of(), token)).get("me").toString())
                .contains("student@lms.local");
        assertThat(data(gql("{ myEnrollments { status course { title } } }", Map.of(), token)).toString())
                .contains("Основы Python");
        assertThat(redis.opsForValue().get(CatalogCache.COURSE + Ids.COURSE)).contains("Основы Python");
        assertThat(data(gql(
                "query($id: ID!) { myProgress(courseId: $id) { percent } }",
                Map.of("id", Ids.COURSE),
                token
        )).toString()).contains("50");
        assertThat(data(gql("{ myNotifications { type } }", Map.of(), token)).toString()).contains("GradeAssigned");
        assertThat(data(gql(
                "query($id: ID!) { attemptJournal(courseId: $id) { grade { score } } }",
                Map.of("id", Ids.COURSE),
                token
        )).toString()).contains("8");
        assertThat(data(gql(
                "query($id: ID!) { courseAnalytics(courseId: $id) { enrollCount } }",
                Map.of("id", Ids.COURSE),
                token
        )).toString()).contains("enrollCount");
    }

    @Test
    void errors() {
        Map<String, Object> anonymous = gql("{ me { email } }");
        assertThat(anonymous.get("errors")).isNotNull();

        Map<String, Object> noProgress = gql(
                "query($id: ID!) { myProgress(courseId: $id) { percent } }",
                Map.of("id", Ids.DRAFT_COURSE),
                studentToken()
        );
        assertThat(noProgress.get("errors")).isNotNull();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> body) {
        assertThat(body.get("errors")).isNull();
        return (Map<String, Object>) body.get("data");
    }
}
