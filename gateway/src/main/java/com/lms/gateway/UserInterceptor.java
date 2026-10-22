package com.lms.gateway;

import com.lms.shared.Ids;
import com.lms.shared.IdentityPort;
import com.lms.shared.UserSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserInterceptor implements WebGraphQlInterceptor {
    private static final String BEARER = "Bearer ";

    private final IdentityPort identity;

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        UserSnapshot user = resolveUser(request.getHeaders());
        request.configureExecutionInput((input, builder) -> builder.graphQLContext(ctx -> {
            if (user != null) {
                ctx.put("user", user);
            }
        }).build());
        return chain.next(request);
    }

    private UserSnapshot resolveUser(HttpHeaders headers) {
        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER)) {
            return identity.validateSession(authorization.substring(BEARER.length()).trim());
        }
        // X-User-Id и студент по умолчанию — отладочный вход для GraphiQL без login.
        String userId = headers.getFirst("X-User-Id");
        return identity.getUser(userId == null || userId.isBlank() ? Ids.STUDENT : userId.trim());
    }
}
