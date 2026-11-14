package com.lms.gateway;

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
    private final EventDrain events;

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String token = bearer(request.getHeaders());
        UserSnapshot user = token == null ? null : identity.validateSession(token);
        request.configureExecutionInput((input, builder) -> builder.graphQLContext(ctx -> {
            if (token != null) {
                ctx.put("token", token);
            }
            if (user != null) {
                ctx.put("user", user);
            }
        }).build());
        return chain.next(request).flatMap(response -> {
            events.awaitSettled();
            return Mono.just(response);
        });
    }

    private static String bearer(HttpHeaders headers) {
        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER)) {
            return null;
        }
        String token = authorization.substring(BEARER.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
