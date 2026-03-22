package com.crishof.traveldeskapi.security.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final String ERROR_FORBIDDEN = "Forbidden";
    private static final String MESSAGE_ACCESS_DENIED = "You do not have permission to access this resource";

    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        securityErrorResponseWriter.write(
                request,
                response,
                HttpStatus.FORBIDDEN,
                ERROR_FORBIDDEN,
                MESSAGE_ACCESS_DENIED);
    }
}

