package com.lms;

import com.lms.grpc.GrpcCalls;
import com.lms.grpc.GrpcProperties;
import com.lms.shared.AppException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GrpcRetryTest {

    @Test
    void temporaryFailureIsRetriedThenSucceeds() {
        GrpcProperties properties = policy();
        AtomicInteger calls = new AtomicInteger();
        String value = GrpcCalls.call("catalog", "GetCourse", properties, () -> {
            if (calls.incrementAndGet() < 3) {
                throw Status.UNAVAILABLE.withDescription("connection refused").asRuntimeException();
            }
            return "ok";
        });
        assertThat(value).isEqualTo("ok");
        assertThat(calls).hasValue(3);
    }

    @Test
    void businessErrorIsNotRetried() {
        GrpcProperties properties = policy();
        AtomicInteger calls = new AtomicInteger();
        assertThatThrownBy(() -> GrpcCalls.call("catalog", "GetCourse", properties, () -> {
            calls.incrementAndGet();
            throw Status.NOT_FOUND.withDescription("Курс не найден").asRuntimeException();
        })).isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo("NOT_FOUND");
        assertThat(calls).hasValue(1);
    }

    @Test
    void retriesStopAtTheLimit() {
        GrpcProperties properties = policy();
        AtomicInteger calls = new AtomicInteger();
        assertThatThrownBy(() -> GrpcCalls.call("catalog", "GetCourse", properties, () -> {
            calls.incrementAndGet();
            throw new StatusRuntimeException(Status.DEADLINE_EXCEEDED);
        })).isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo("UNAVAILABLE");
        assertThat(calls).hasValue(3);
    }

    private static GrpcProperties policy() {
        GrpcProperties properties = new GrpcProperties();
        properties.setRetryMax(2);
        properties.setRetryPause(Duration.ZERO);
        return properties;
    }
}
