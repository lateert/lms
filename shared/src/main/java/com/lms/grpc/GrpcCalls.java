package com.lms.grpc;

import com.lms.shared.AppException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.function.Supplier;

public final class GrpcCalls {
    private GrpcCalls() {
    }

    public static <T> T call(String service, Supplier<T> action) {
        try {
            return action.get();
        } catch (StatusRuntimeException ex) {
            Status.Code code = ex.getStatus().getCode();
            String description = ex.getStatus().getDescription();
            if (description != null && !description.isBlank() && code != Status.Code.UNAVAILABLE) {
                throw new AppException(description, switch (code) {
                    case NOT_FOUND -> "NOT_FOUND";
                    case UNAUTHENTICATED -> "UNAUTHORIZED";
                    case PERMISSION_DENIED -> "FORBIDDEN";
                    case ALREADY_EXISTS -> "CONFLICT";
                    default -> "BAD_REQUEST";
                });
            }
            throw AppException.unavailable("Сервис " + service + " недоступен");
        }
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
}
