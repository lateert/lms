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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GraphqlSecurityTest {

    @Autowired
    private TestRestTemplate rest;

    @SuppressWarnings("unchecked")
    private Map<String, Object> gql(String token, String query, Map<String, Object> variables) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        ResponseEntity<Map> response = rest.postForEntity(
                "/graphql", new HttpEntity<>(Map.of("query", query, "variables", variables), headers), Map.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private String login(String email) {
        Map<String, Object> body = gql(null,
                "mutation($in: LoginInput!) { login(input: $in) { token expiresAt user { roles } } }",
                Map.of("in", Map.of("email", email, "password", Ids.DEMO_PASSWORD)));
        assertThat(body.get("errors")).isNull();
        return (String) ((Map<String, Object>) ((Map<String, Object>) body.get("data")).get("login")).get("token");
    }

    @SuppressWarnings("unchecked")
    private String code(String token, String query) {
        List<Map<String, Object>> errors = (List<Map<String, Object>>) gql(token, query, Map.of()).get("errors");
        assertThat(errors).isNotEmpty();
        return (String) ((Map<String, Object>) errors.get(0).get("extensions")).get("code");
    }

    @Test
    void jwtRequiresActiveRedisSession() {
        String token = login("student@lms.local");
        assertThat(token.split("\\.")).hasSize(3);

        Map<String, Object> me = gql(token, "{ me { email roles } }", Map.of());
        assertThat(me.get("errors")).isNull();
        assertThat(me.toString()).contains("student@lms.local", "student");

        assertThat(code(null, "{ me { email } }")).isEqualTo("UNAUTHORIZED");
        assertThat(code(token + "x", "{ me { email } }")).isEqualTo("UNAUTHORIZED");

        Map<String, Object> loggedOut = gql(token, "mutation { logout }", Map.of());
        assertThat(loggedOut.get("errors")).isNull();
        assertThat(code(token, "{ me { email } }")).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void roleGatesMutations() {
        String student = login("student@lms.local");
        String teacher = login("teacher@lms.local");
        assertThat(code(student, "mutation { createCourse(input: {categoryId: \"44444444-4444-4444-4444-444444444444\", title: \"X\", difficulty: \"beginner\"}) { id } }"))
                .isEqualTo("FORBIDDEN");
        assertThat(code(teacher, "mutation { enrollCourse(courseId: \"" + Ids.COURSE + "\") { id } }"))
                .isEqualTo("FORBIDDEN");
        assertThat(gql(null, "{ catalog { title } }", Map.of()).get("errors")).isNull();
    }
}
