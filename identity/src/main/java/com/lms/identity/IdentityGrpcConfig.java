package com.lms.identity;

import com.lms.grpc.Empty;
import com.lms.grpc.GrpcCalls;
import com.lms.grpc.GrpcMapping;
import com.lms.grpc.GrpcProperties;
import com.lms.grpc.GrpcServer;
import com.lms.grpc.IdentityApiGrpc;
import com.lms.grpc.TokenRequest;
import com.lms.grpc.UserIdRequest;
import com.lms.grpc.UserReply;
import io.grpc.stub.StreamObserver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdentityGrpcConfig {
    @Bean
    GrpcServer identityGrpcServer(GrpcProperties properties, IdentityService identity) {
        return new GrpcServer(properties.getIdentity(), new IdentityApiGrpc.IdentityApiImplBase() {
            @Override
            public void validateSession(TokenRequest request, StreamObserver<UserReply> response) {
                reply(response, () -> GrpcMapping.userReply(identity.validateSession(request.getToken())));
            }

            @Override
            public void getUser(UserIdRequest request, StreamObserver<UserReply> response) {
                reply(response, () -> GrpcMapping.userReply(identity.getUser(request.getUserId())));
            }

            @Override
            public void logout(TokenRequest request, StreamObserver<Empty> response) {
                reply(response, () -> {
                    identity.logout(request.getToken());
                    return Empty.getDefaultInstance();
                });
            }
        });
    }

    private static <T> void reply(StreamObserver<T> response, java.util.function.Supplier<T> body) {
        try {
            response.onNext(GrpcCalls.guard(body));
            response.onCompleted();
        } catch (RuntimeException ex) {
            response.onError(ex);
        }
    }
}
