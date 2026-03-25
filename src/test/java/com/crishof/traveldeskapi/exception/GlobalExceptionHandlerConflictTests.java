package com.crishof.traveldeskapi.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerConflictTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldMapAgencyAlreadyExistExceptionToHttpConflict() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/signup");

        ResponseEntity<ApiError> response = handler.handleConflict(
                new AgencyAlreadyExistException("Agency Test already exists"),
                request
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertEquals("Conflict", response.getBody().error());
        assertEquals("Agency Test already exists", response.getBody().message());
        assertEquals("/api/v1/auth/signup", response.getBody().path());
    }

    @Test
    void shouldMapConflictExceptionToHttpConflict() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/suppliers");

        ResponseEntity<ApiError> response = handler.handleConflict(
                new ConflictException("Supplier email already in use"),
                request
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertEquals("Conflict", response.getBody().error());
        assertEquals("Supplier email already in use", response.getBody().message());
        assertEquals("/api/v1/suppliers", response.getBody().path());
    }
}

