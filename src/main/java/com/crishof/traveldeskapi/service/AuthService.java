package com.crishof.traveldeskapi.service;


import com.crishof.traveldeskapi.dto.*;
import com.crishof.traveldeskapi.security.principal.SecurityUser;

import java.util.UUID;

public interface AuthService {

    SignupResponse signup(SignupRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);

    AuthResponse verifyEmail(VerifyEmailRequest request);

    InviteInfoResponse getInviteInfo(String token);

    AuthResponse acceptInvite(AcceptInviteRequest request);

    void forgotPassword(String email);

    void resetPassword(ResetPasswordRequest request);

    void logout(String refreshToken);

    void logoutAll(UUID userId);

    AuthMeResponse me(SecurityUser securityUser);

    InvitationResponse createInvitation(UUID agencyId, UUID invitedByUserId, CreateInvitationRequest request);
}
