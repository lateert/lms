package com.lms.grpc;

import com.lms.shared.AppException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public final class GrpcCalls {
    private static final Logger log = LoggerFactory.getLogger(GrpcCalls.class);

    private GrpcCalls() {
    }

    public static <T> T call(String service, String operation, GrpcProperties properties, Supplier<T> action) {
        int attempts = Math.max(1, properties.getRetryMax() + 1);
        StatusRuntimeException last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            long started = System.nanoTime();
            try {
                T value = action.get();
                log.info("gRPC {}.{} выполнен за {} мс, попытка {}", service, operation, millis(started), attempt);
                return value;
            } catch (StatusRuntimeException ex) {
                last = ex;
                if (!temporary(ex) || attempt == attempts) {
                    log.error("gRPC {}.{} ошибка {} за {} мс, попытка {} из {}",
                            service, operation, ex.getStatus().getCode(), millis(started), attempt, attempts);
                    throw map(service, ex);
                }
                log.warn("gRPC {}.{} временная ошибка {}, повтор {} из {}",
                        service, operation, ex.getStatus().getCode(), attempt, attempts);
                pause(properties);
            }
        }
        throw map(service, last);
    }

    public static <T> T guard(Supplier<T> action) {
        try {
            return action.get();
        } catch (AppException ex) {
            Status status = switch (ex.getCode()) {
                case "NOT_FOUND" -> Status.NOT_FOUND;
                case "UNAUTHORIZED" -> Status.UNAUTHENTICATED;
                case "FORBIDDEN" -> Status.PERMISSION_DENIED;
                case "CONFLICT" -> Status.ALREADY_EXISTS;
                case "UNAVAILABLE" -> Status.UNAVAILABLE;
                default -> Status.INVALID_ARGUMENT;
            };
            throw status.withDescription(ex.getMessage()).asRuntimeException();
        }
    }

    private static boolean temporary(StatusRuntimeException ex) {
        Status.Code code = ex.getStatus().getCode();
        return code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED;
    }

    private static AppException map(String service, StatusRuntimeException ex) {
        Status.Code code = ex.getStatus().getCode();
        if (temporary(ex)) {
            String reason = code == Status.Code.DEADLINE_EXCEEDED ? "не ответил вовремя" : "недоступен";
            return AppException.unavailable("Сервис " + service + " " + reason);
        }
        String description = ex.getStatus().getDescription();
        if (description != null && !description.isBlank()) {
            String appCode = switch (code) {
                case NOT_FOUND -> "NOT_FOUND";
                case UNAUTHENTICATED -> "UNAUTHORIZED";
                case PERMISSION_DENIED -> "FORBIDDEN";
                case ALREADY_EXISTS -> "CONFLICT";
                default -> "BAD_REQUEST";
            };
            return new AppException(description, appCode);
        }
        return AppException.unavailable("Сервис " + service + " недоступен");
    }

    private static void pause(GrpcProperties properties) {
        long pause = properties.getRetryPause() == null ? 0 : properties.getRetryPause().toMillis();
        if (pause <= 0) {
            return;
        }
        try {
            Thread.sleep(pause);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw AppException.unavailable("Повтор gRPC прерван");
        }
    }

    private static long millis(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}
