package com.crishof.traveldeskapi.controller;

import com.crishof.traveldeskapi.dto.*;
import com.crishof.traveldeskapi.security.principal.SecurityUser;
import com.crishof.traveldeskapi.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    //  ===============
    //  GET ACCOUNT
    //  ===============

    @Operation(summary = "Get account")
    @ApiResponse(responseCode = "200", description = "Account retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<AccountResponse> getAccount(@AuthenticationPrincipal SecurityUser securityUser) {
        log.info("Get account request received for userId={}", securityUser.getId());
        return ResponseEntity.ok(accountService.getAccount(securityUser.getId()));
    }

    //  ===============
    //  UPDATE ACCOUNT
    //  ===============

    @Operation(summary = "Update account")
    @ApiResponse(responseCode = "200", description = "Account updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("isAuthenticated()")
    @PutMapping
    public ResponseEntity<AccountResponse> updateAccount(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody AccountRequest request
    ) {
        log.info("Update account request received for userId={}, email={}", securityUser.getId(), request.email());
        return ResponseEntity.ok(accountService.updateAccount(securityUser.getId(), request));
    }

    //  ===============
    //  GET AGENCY SETTINGS
    //  ===============

    @Operation(summary = "Get agency settings")
    @ApiResponse(responseCode = "200", description = "Agency settings retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/agency")
    public ResponseEntity<AgencySettingsResponse> getAgencySettings(@AuthenticationPrincipal SecurityUser securityUser) {
        log.info("Get agency settings request received for userId={}, agencyId={}", securityUser.getId(), securityUser.getAgencyId());
        return ResponseEntity.ok(accountService.getAgencySettings(securityUser.getAgencyId()));
    }

    //  ===============
    //  UPDATE AGENCY SETTINGS
    //  ===============

    @Operation(summary = "Update agency settings")
    @ApiResponse(responseCode = "200", description = "Agency settings updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/agency")
    public ResponseEntity<AgencySettingsResponse> updateAgencySettings(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody AgencySettingsRequest request
    ) {
        log.info("Update agency settings request received for userId={}, agencyId={}", securityUser.getId(), securityUser.getAgencyId());
        return ResponseEntity.ok(accountService.updateAgencySettings(securityUser.getAgencyId(), request));
    }

    //  ===============
    //  GET COMMISSION SETTINGS
    //  ===============

    @Operation(summary = "Get commission settings")
    @ApiResponse(responseCode = "200", description = "Commission settings retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/commission")
    public ResponseEntity<CommissionSettingsResponse> getCommissionSettings(@AuthenticationPrincipal SecurityUser securityUser) {
        log.info("Get commission settings request received for userId={}, agencyId={}", securityUser.getId(), securityUser.getAgencyId());
        return ResponseEntity.ok(accountService.getCommissionSettings(securityUser.getAgencyId()));
    }

    //  ===============
    //  UPDATE COMMISSION SETTINGS
    //  ===============

    @Operation(summary = "Update commission settings")
    @ApiResponse(responseCode = "200", description = "Commission settings updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/commission")
    public ResponseEntity<CommissionSettingsResponse> updateCommissionSettings(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody CommissionSettingsRequest request
    ) {
        log.info("Update commission settings request received for userId={}, agencyId={}", securityUser.getId(), securityUser.getAgencyId());
        return ResponseEntity.ok(accountService.updateCommissionSettings(securityUser.getAgencyId(), request));
    }
}