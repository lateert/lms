package com.lms.grpc;

import com.lms.shared.IdentityPort;
import com.lms.shared.UserSnapshot;
import io.grpc.ManagedChannel;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class GrpcIdentityClient implements IdentityPort {
    private final ManagedChannel channel;
    private final IdentityApiGrpc.IdentityApiBlockingStub stub;

    public GrpcIdentityClient(GrpcProperties properties) {
        this.channel = GrpcMapping.channel(properties.getIdentity());
        this.stub = IdentityApiGrpc.newBlockingStub(channel);
    }

    @Override
    public UserSnapshot validateSession(String token) {
        return GrpcMapping.user(GrpcCalls.call("identity", () -> stub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .validateSession(TokenRequest.newBuilder().setToken(token == null ? "" : token).build())));
    }

    @Override
    public void logout(String token) {
        GrpcCalls.call("identity", () -> stub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .logout(TokenRequest.newBuilder().setToken(token == null ? "" : token).build()));
    }

    @Override
    public UserSnapshot getUser(String userId) {
        return GrpcMapping.user(GrpcCalls.call("identity", () -> stub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .getUser(UserIdRequest.newBuilder().setUserId(userId).build())));
    }

    @PreDestroy
    public void close() {
        GrpcMapping.shutdown(channel);
    }
}
