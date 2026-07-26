package org.example.contest;

import org.springframework.http.HttpStatus;

public record ServiceResult<T>(T value, HttpStatus errorStatus, String errorCode) {

    public static <T> ServiceResult<T> ok(T value) {
        return new ServiceResult<>(value, null, null);
    }

    public static <T> ServiceResult<T> err(HttpStatus status, String code) {
        return new ServiceResult<>(null, status, code);
    }

    public boolean isOk() {
        return errorCode == null;
    }
}
