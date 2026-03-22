package com.crishof.traveldeskapi.security.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String ERROR_UNAUTHORIZED = "Unauthorized";
    private static final String MESSAGE_AUTH_REQUIRED = "Authentication is required to access this resource";

    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        securityErrorResponseWriter.write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                ERROR_UNAUTHORIZED,
                MESSAGE_AUTH_REQUIRED);
    }
}

