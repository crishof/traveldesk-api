package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.dto.*;
import com.crishof.traveldeskapi.exception.ForbiddenOperationException;
import com.crishof.traveldeskapi.exception.InvalidRequestException;
import com.crishof.traveldeskapi.exception.ResourceNotFoundException;
import com.crishof.traveldeskapi.model.User;
import com.crishof.traveldeskapi.model.UserStatus;
import com.crishof.traveldeskapi.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamServiceImpl implements TeamService {

    private final UserRepository userRepository;

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<TeamMemberResponse> getMembers(UUID agencyId) {
        validateAgencyId(agencyId);

        return userRepository.findAllByAgencyIdOrderByFullNameAsc(agencyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public TeamMemberResponse updateMember(UUID agencyId, UUID memberId, TeamMemberRequest request) {
        validateAgencyId(agencyId);
        validateUserId(memberId);

        User member = getUserByAgencyOrThrow(agencyId, memberId);

        member.setRole(parseRole(request.role()));
        member.setStatus(parseStatus(request.status()));

        return toResponse(userRepository.save(member));
    }

    @Override
    public TeamMemberResponse updateCommission(UUID agencyId, UUID memberId, TeamMemberCommissionRequest request) {
        validateAgencyId(agencyId);
        validateUserId(memberId);

        User member = getUserByAgencyOrThrow(agencyId, memberId);
        member.setCommissionPercentage(request.commissionPercentage());

        return toResponse(userRepository.save(member));
    }

    @Override
    public void removeMember(UUID agencyId, UUID memberId, UUID currentUserId) {
        validateAgencyId(agencyId);
        validateUserId(memberId);
        validateUserId(currentUserId);

        if (memberId.equals(currentUserId)) {
            throw new ForbiddenOperationException("You cannot remove your own account from the team");
        }

        User member = getUserByAgencyOrThrow(agencyId, memberId);
        userRepository.delete(member);
    }

    @Override
    public MessageResponse inviteMember(UUID agencyId, UUID invitedByUserId, TeamInviteRequest request) {
        validateAgencyId(agencyId);
        validateUserId(invitedByUserId);

        throw new InvalidRequestException("Team invitation flow is not implemented yet");
    }

    @Override
    public MessageResponse acceptInvite(AcceptInviteRequest request) {
        throw new InvalidRequestException("Invite acceptance flow is not implemented yet");
    }

    private User getUserByAgencyOrThrow(UUID agencyId, UUID userId) {
        return userRepository.findByIdAndAgencyId(userId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Team member not found with id: " + userId));
    }

    private com.crishof.traveldeskapi.model.Role parseRole(String role) {
        try {
            return com.crishof.traveldeskapi.model.Role.valueOf(normalizeEnumValue(role));
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Invalid role: " + role);
        }
    }

    private UserStatus parseStatus(String status) {
        try {
            return UserStatus.valueOf(normalizeEnumValue(status));
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Invalid user status: " + status);
        }
    }

    private TeamMemberResponse toResponse(User user) {
        return new TeamMemberResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getCommissionPercentage()
        );
    }

    private String normalizeEnumValue(String value) {
        return value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }

    private void validateAgencyId(UUID agencyId) {
        if (agencyId == null) {
            throw new InvalidRequestException("Agency id is required");
        }
    }

    private void validateUserId(UUID userId) {
        if (userId == null) {
            throw new InvalidRequestException("User id is required");
        }
    }
}
