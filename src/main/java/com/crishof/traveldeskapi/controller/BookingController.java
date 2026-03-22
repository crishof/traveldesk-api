package com.crishof.traveldeskapi.controller;

import com.crishof.traveldeskapi.dto.BookingRequest;
import com.crishof.traveldeskapi.dto.BookingResponse;
import com.crishof.traveldeskapi.security.principal.SecurityUser;
import com.crishof.traveldeskapi.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    //  ===============
    //  GET BOOKINGS
    //  ===============

    @Operation(summary = "Get bookings")
    @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<java.util.List<BookingResponse>> getBookings(@AuthenticationPrincipal SecurityUser securityUser) {
        log.info("Get bookings request received for userId={}", securityUser.getId());
        return ResponseEntity.ok(bookingService.getAll(securityUser.getAgencyId()));
    }

    //  ===============
    //  CREATE BOOKING
    //  ===============

    @Operation(summary = "Create a new booking")
    @ApiResponse(responseCode = "201", description = "Booking created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody BookingRequest request
    ) {
        log.info("Create booking request received for userId={}, reference={}", securityUser.getId(), request.reference());
        return ResponseEntity.status(HttpStatusCode.valueOf(201))
                .body(bookingService.create(securityUser.getAgencyId(), securityUser.getId(), request));
    }

    //  ===============
    //  UPDATE BOOKING
    //  ===============

    @Operation(summary = "Update a booking")
    @ApiResponse(responseCode = "200", description = "Booking updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ResponseEntity<BookingResponse> updateBooking(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable UUID id,
            @Valid @RequestBody BookingRequest request
    ) {
        log.info("Update booking request received for userId={}, bookingId={}", securityUser.getId(), id);
        return ResponseEntity.ok(bookingService.update(securityUser.getAgencyId(), id, request));
    }

    //  ===============
    //  GET BOOKING BY ID
    //  ===============

    @Operation(summary = "Get a booking by ID")
    @ApiResponse(responseCode = "200", description = "Booking found successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable UUID id
    ) {
        log.info("Get booking by ID request received for userId={}, bookingId={}", securityUser.getId(), id);
        return ResponseEntity.ok(bookingService.findById(securityUser.getAgencyId(), id));
    }

    //  ===============
    //  DELETE BOOKING
    //  ===============

    @Operation(summary = "Delete a booking")
    @ApiResponse(responseCode = "204", description = "Booking deleted successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable UUID id
    ) {
        log.info("Delete booking request received for userId={}, bookingId={}", securityUser.getId(), id);
        bookingService.delete(securityUser.getAgencyId(), id);
        return ResponseEntity.noContent().build();
    }
}