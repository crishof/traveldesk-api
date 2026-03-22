package com.crishof.traveldeskapi.security.web;

import com.crishof.traveldeskapi.exception.ApiError;
import com.crishof.traveldeskapi.exception.ApiErrorFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityErrorResponseWriter {

    private final ApiErrorFactory apiErrorFactory;

    public void write(HttpServletRequest request,
                      HttpServletResponse response,
                      HttpStatusCode status,
                      String error,
                      String message) throws IOException {

        log.debug("Writing security error response for status {} and error {}", status, error);
        ApiError apiError = apiErrorFactory.build(status, error, message, request);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(toJson(apiError));
    }

    private String toJson(ApiError error) {
        log.debug("Converting ApiError to JSON: {}", error);
        return """
                {
                  "timestamp":"%s",
                  "status":%d,
                  "error":"%s",
                  "message":"%s",
                  "path":"%s"
                }
                """.formatted(escape(error.timestamp().toString()),
                error.status(), escape(error.error()), escape(error.message()), escape(error.path()));
    }

    private String escape(String value) {
        log.debug("Escaping value: {}", value);
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

