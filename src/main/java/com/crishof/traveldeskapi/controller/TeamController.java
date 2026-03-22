package com.crishof.traveldeskapi.controller;

import com.crishof.traveldeskapi.dto.*;
import com.crishof.traveldeskapi.security.principal.SecurityUser;
import com.crishof.traveldeskapi.service.TeamService;
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
@RequestMapping("/api/v1/team")
@RequiredArgsConstructor
@Slf4j
public class TeamController {

    private final TeamService teamService;

    //  ===============
    //  GET TEAM MEMBERS
    //  ===============

    @Operation(summary = "Get team members")
    @ApiResponse(responseCode = "200", description = "Team members retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<java.util.List<TeamMemberResponse>> getTeamMembers(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        log.info("Get team members request received for userId={}, agencyId={}", securityUser.getId(), securityUser.getAgencyId());
        return ResponseEntity.ok(teamService.getMembers(securityUser.getAgencyId()));
    }

    //  ===============
    //  INVITE TEAM MEMBER
    //  ===============

    @Operation(summary = "Invite a team member")
    @ApiResponse(responseCode = "201", description = "Invitation sent successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/invite")
    public ResponseEntity<MessageResponse> inviteMember(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody TeamInviteRequest request
    ) {
        log.info("Invite team member request received for userId={}, agencyId={}, email={}",
                securityUser.getId(), securityUser.getAgencyId(), request.email());

        return ResponseEntity.status(HttpStatusCode.valueOf(201))
                .body(teamService.inviteMember(securityUser.getAgencyId(), securityUser.getId(), request));
    }

    //  ===============
    //  UPDATE TEAM MEMBER
    //  ===============

    @Operation(summary = "Update a team member")
    @ApiResponse(responseCode = "200", description = "Team member updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Team member not found")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ResponseEntity<TeamMemberResponse> updateMember(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable UUID id,
            @Valid @RequestBody TeamMemberRequest request
    ) {
        log.info("Update team member request received for userId={}, agencyId={}, memberId={}",
                securityUser.getId(), securityUser.getAgencyId(), id);

        return ResponseEntity.ok(teamService.updateMember(securityUser.getAgencyId(), id, request));
    }

        @Operation(summary = "Update team member commission")
        @ApiResponse(responseCode = "200", description = "Team member commission updated successfully")
        @ApiResponse(responseCode = "400", description = "Invalid request")
        @ApiResponse(responseCode = "401", description = "Unauthorized")
        @ApiResponse(responseCode = "404", description = "Team member not found")
        @PreAuthorize("isAuthenticated()")
        @PatchMapping("/{id}/commission")
        public ResponseEntity<TeamMemberResponse> updateCommission(
                        @AuthenticationPrincipal SecurityUser securityUser,
                        @PathVariable UUID id,
                        @Valid @RequestBody TeamMemberCommissionRequest request
        ) {
                log.info("Update team member commission request received for userId={}, agencyId={}, memberId={}",
                                securityUser.getId(), securityUser.getAgencyId(), id);

                return ResponseEntity.ok(teamService.updateCommission(securityUser.getAgencyId(), id, request));
        }

    //  ===============
    //  DELETE TEAM MEMBER
    //  ===============

    @Operation(summary = "Delete a team member")
    @ApiResponse(responseCode = "204", description = "Team member deleted successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Team member not found")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable UUID id
    ) {
        log.info("Delete team member request received for userId={}, agencyId={}, memberId={}",
                securityUser.getId(), securityUser.getAgencyId(), id);

        teamService.removeMember(securityUser.getAgencyId(), id, securityUser.getId());
        return ResponseEntity.noContent().build();
    }
}