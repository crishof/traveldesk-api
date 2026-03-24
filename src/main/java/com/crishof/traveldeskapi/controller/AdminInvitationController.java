package com.crishof.traveldeskapi.controller;

import com.crishof.traveldeskapi.dto.CreateInvitationRequest;
import com.crishof.traveldeskapi.dto.InvitationResponse;
import com.crishof.traveldeskapi.security.principal.SecurityUser;
import com.crishof.traveldeskapi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/invitations")
@RequiredArgsConstructor
@Slf4j
public class AdminInvitationController {

    private final AuthService authService;

//  ===========
//    CREATE INVITATION
//  ===========
    @Operation(summary = "Create invitation", description = "Creates a new invitation and sends it by email")
    @ApiResponse(responseCode = "201", description = "Invitation created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @PostMapping
    public ResponseEntity<InvitationResponse> createInvitation(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody CreateInvitationRequest request
    ) {
        log.info("Creating invitation for email={} with role={}", request.email(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.createInvitation(securityUser.getAgencyId(), securityUser.getId(), request));
    }
}
