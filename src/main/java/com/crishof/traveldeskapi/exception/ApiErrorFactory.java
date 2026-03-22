package com.crishof.traveldeskapi.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ApiErrorFactory {

    public ApiError build(HttpStatusCode status, String error, String message, HttpServletRequest request) {
        return new ApiError(
                Instant.now(),
                status.value(),
                error,
                message,
                request.getRequestURI()
        );
    }
}
