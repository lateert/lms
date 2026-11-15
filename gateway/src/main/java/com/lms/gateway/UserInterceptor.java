package com.lms.gateway;

import com.lms.shared.IdentityPort;
import com.lms.shared.UserSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
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
        String scenario = UUID.randomUUID().toString().substring(0, 8);
        request.configureExecutionInput((input, builder) -> builder.graphQLContext(ctx -> {
            ctx.put("scenario", scenario);
            if (token != null) {
                ctx.put("token", token);
            }
            if (user != null) {
                ctx.put("user", user);
            }
        }).build());
        return Mono.defer(() -> {
            MDC.put("scenario", scenario);
            log.info("Сценарий {} начат, пользователь {}", scenario, user == null ? "гость" : user.email());
            return chain.next(request).flatMap(response -> {
                events.awaitSettled();
                if (response.getErrors().isEmpty()) {
                    log.info("Сценарий {} завершён", scenario);
                } else {
                    log.warn("Сценарий {} завершён с ошибкой: {}", scenario, response.getErrors().get(0).getMessage());
                }
                return Mono.just(response);
            });
        }).doFinally(signal -> MDC.remove("scenario"));
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
