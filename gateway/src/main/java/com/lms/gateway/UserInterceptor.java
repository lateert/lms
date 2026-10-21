package com.lms.gateway;

import com.lms.shared.Ids;
import com.lms.shared.IdentityPort;
import com.lms.shared.UserSnapshot;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserInterceptor implements WebGraphQlInterceptor {
    private final IdentityPort identity;

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String header = request.getHeaders().getFirst("X-User-Id");
        String userId = (header == null || header.isBlank()) ? Ids.STUDENT : header.trim();
        UserSnapshot user = identity.validateSession(userId);
        request.configureExecutionInput((input, builder) -> builder.graphQLContext(ctx -> {
            ctx.put("userId", userId);
            if (user != null) {
                ctx.put("user", user);
            }
        }).build());
        return chain.next(request);
    }
}
