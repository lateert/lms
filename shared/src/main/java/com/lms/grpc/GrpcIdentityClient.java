package com.lms.grpc;

import com.lms.shared.IdentityPort;
import com.lms.shared.UserSnapshot;
import io.grpc.ManagedChannel;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class GrpcIdentityClient implements IdentityPort {
    private final GrpcProperties properties;
    private final ManagedChannel channel;
    private final IdentityApiGrpc.IdentityApiBlockingStub stub;

    public GrpcIdentityClient(GrpcProperties properties) {
        this.properties = properties;
        this.channel = GrpcMapping.channel(properties.getIdentity());
        this.stub = IdentityApiGrpc.newBlockingStub(channel);
    }

    @Override
    public UserSnapshot validateSession(String token) {
        return GrpcMapping.user(call("ValidateSession", () -> stub.withDeadlineAfter(properties.getTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .validateSession(TokenRequest.newBuilder().setToken(token == null ? "" : token).build())));
    }

    @Override
    public void logout(String token) {
        call("Logout", () -> stub.withDeadlineAfter(properties.getTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .logout(TokenRequest.newBuilder().setToken(token == null ? "" : token).build()));
    }

    @Override
    public UserSnapshot getUser(String userId) {
        return GrpcMapping.user(call("GetUser", () -> stub.withDeadlineAfter(properties.getTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .getUser(UserIdRequest.newBuilder().setUserId(userId).build())));
    }

    private <T> T call(String operation, java.util.function.Supplier<T> action) {
        return GrpcCalls.call("identity", operation, properties, action);
    }

    @PreDestroy
    public void close() {
        GrpcMapping.shutdown(channel);
    }
}
