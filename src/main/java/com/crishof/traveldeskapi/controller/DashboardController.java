package com.crishof.traveldeskapi.controller;

import com.crishof.traveldeskapi.dto.DashboardStatsResponse;
import com.crishof.traveldeskapi.security.principal.SecurityUser;
import com.crishof.traveldeskapi.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    //  ===============
    //  GET DASHBOARD STATS
    //  ===============

    @Operation(summary = "Get dashboard statistics")
    @ApiResponse(responseCode = "200", description = "Dashboard statistics retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats(@AuthenticationPrincipal SecurityUser securityUser) {
        log.info("Dashboard stats request received for userId={}, agencyId={}", securityUser.getId(), securityUser.getAgencyId());
        return ResponseEntity.ok(dashboardService.getStats(securityUser.getAgencyId()));
    }
}
