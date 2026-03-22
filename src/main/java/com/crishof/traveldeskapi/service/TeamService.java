package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.dto.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface TeamService {

    List<TeamMemberResponse> getMembers(UUID agencyId);

    TeamMemberResponse updateMember(UUID agencyId, UUID memberId, @Valid TeamMemberRequest request);

    TeamMemberResponse updateCommission(UUID agencyId, UUID memberId, @Valid TeamMemberCommissionRequest request);

    void removeMember(UUID agencyId, UUID memberId, UUID currentUserId);

    MessageResponse inviteMember(UUID agencyId, UUID invitedByUserId, @Valid TeamInviteRequest request);

    MessageResponse acceptInvite(@Valid AcceptInviteRequest request);
}
