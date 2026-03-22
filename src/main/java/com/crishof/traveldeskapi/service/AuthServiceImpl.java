package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.dto.*;
import com.crishof.traveldeskapi.exception.*;
import com.crishof.traveldeskapi.model.*;
import com.crishof.traveldeskapi.repository.*;
import com.crishof.traveldeskapi.security.jwt.JwtService;
import com.crishof.traveldeskapi.security.principal.SecurityUser;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final int VERIFICATION_CODE_DIGITS = 6;
    private static final long PASSWORD_RESET_MINUTES = 30;
    private static final long INVITATION_DAYS = 7;

    private final UserRepository userRepository;
    private final SecurityAccountRepository securityAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final InvitationTokenRepository invitationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final JwtService jwtService;

    @Value("${app.email-verification.code-ttl-minutes:10}")
    private long emailVerificationCodeTtlMinutes;

//  ===========
//    SIGNUP
//  ===========
    @Override
    public SignupResponse signup(SignupRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        log.debug("signup requested for email={}", normalizedEmail);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.debug("signup rejected because email already exists: {}", normalizedEmail);
            throw new EmailAlreadyExistException("Email " + normalizedEmail + " is already in use");
        }

        User user = new User();
        user.setFullName(normalizeFullName(request.fullName()));
        user.setEmail(normalizedEmail);
        user.setRole(Role.USER);
        user.setStatus(UserStatus.PENDING_VERIFICATION);

        User savedUser = userRepository.save(user);
        log.debug("user persisted for signup id={} email={}", savedUser.getId(), savedUser.getEmail());

        SecurityAccount account = SecurityAccount.builder()
                .user(savedUser)
                .passwordHash(passwordEncoder.encode(request.password()))
                .emailVerified(false)
                .enabled(true)
                .locked(false)
                .build();

        securityAccountRepository.save(account);
        log.debug("security account persisted for userId={}", savedUser.getId());

        issueEmailVerificationCode(savedUser);

        log.info("User signed up successfully. Verification required for email={}", normalizedEmail);

        return new SignupResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                "Signup successful. Please verify your email using the code sent.",
                true);
    }

//  ===========
//    LOGIN
//  ===========
    @Override
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        log.debug("login requested for email={}", normalizedEmail);

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                            normalizedEmail, request.password()));
        } catch (DisabledException ex) {
            log.debug("login rejected by authentication manager because account disabled for email={}", normalizedEmail);
            throw new AccountNotVerifiedException("Email verification is required before login");
        } catch (BadCredentialsException ex) {
            log.debug("login rejected due to bad credentials for email={}", normalizedEmail);
            throw new AuthenticationFailedException("Invalid email or password");
        }

        User user = getUserByEmailOrThrow(normalizedEmail);
        SecurityAccount account = getSecurityAccountByUserOrThrow(user);

        validateAccountCanAuthenticate(user, account);
        log.debug("login successful pre-token checks for userId={}", user.getId());

        return issueAuthTokens(user, account, true);
    }

//  ===========
//    REFRESH
//  ===========
    @Override
    public AuthResponse refreshToken(String refreshTokenValue) {
        log.debug("refreshToken requested");
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            log.debug("refreshToken rejected because token is blank");
            throw new InvalidTokenException("Refresh token is required");
        }

        try {
            if (!jwtService.isTokenValid(refreshTokenValue)) {
                log.debug("refreshToken rejected because JWT validation failed");
                throw new InvalidTokenException("Invalid or expired refresh token");
            }

            RefreshToken storedToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue).orElseThrow(
                    () -> new InvalidTokenException("Invalid or expired refresh token"));

            if (!storedToken.isValid()) {
                log.debug("refreshToken rejected because stored token is no longer valid for userId={}", storedToken.getUser().getId());
                throw new InvalidTokenException("Invalid or expired refresh token");
            }

            User user = storedToken.getUser();
            SecurityAccount account = getSecurityAccountByUserOrThrow(user);

            validateAccountCanAuthenticate(user, account);

            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            log.debug("stored refresh token revoked for userId={}", user.getId());

            return issueAuthTokens(user, account, true);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("refreshToken rejected because parsing/validation threw exception");
            throw new InvalidTokenException("Invalid or expired refresh token");
        }
    }

//  ===========
//    VERIFY EMAIL
//  ===========
    @Override
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        log.debug("verifyEmail requested for email={}", normalizedEmail);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElseThrow(
                () -> new InvalidTokenException("Invalid or expired verification code"));

        SecurityAccount account = getSecurityAccountByUserOrThrow(user);

        EmailVerificationToken verificationToken =
                emailVerificationTokenRepository.findTopByUserAndCodeAndUsedFalseOrderByCreatedAtDesc(user, request.code())
                        .orElseThrow(() -> new InvalidTokenException("Invalid or expired verification code"));

        if (!verificationToken.isValid()) {
            log.debug("verifyEmail rejected because token is expired/used for email={}", normalizedEmail);
            throw new InvalidTokenException("Invalid or expired verification code");
        }

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);
        emailVerificationTokenRepository.deleteByUser(user);
        log.debug("email verification token consumed and cleanup executed for userId={}", user.getId());

        account.setEmailVerified(true);
        securityAccountRepository.save(account);
        log.debug("security account marked as email verified for userId={}", user.getId());

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.debug("user status updated to ACTIVE for userId={}", user.getId());

        return issueAuthTokens(user, account, true);
    }

//  ===========
//    ACCOUNT VERIFICATION
//  ===========
    @Override
    public AuthResponse acceptInvite(AcceptInviteRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        log.debug("acceptInvite requested for email={}", normalizedEmail);

        InvitationToken invitationToken = invitationTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired invitation token"));

        if (!invitationToken.isValid()) {
            log.debug("acceptInvite rejected because invitation token is invalid");
            throw new InvalidTokenException("Invalid or expired invitation token");
        }

        if (!invitationToken.getEmail().equalsIgnoreCase(normalizedEmail)) {
            log.debug("acceptInvite rejected because invitation email mismatch inviteEmail={} requestEmail={}", invitationToken.getEmail(), normalizedEmail);
            throw new BusinessException("Invitation email does not match the provided email");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.debug("acceptInvite rejected because email already exists: {}", normalizedEmail);
            throw new EmailAlreadyExistException("Email " + normalizedEmail + " is already in use");
        }

        User user = new User();
        user.setFullName(normalizeFullName(request.fullName()));
        user.setEmail(normalizedEmail);
        user.setRole(invitationToken.getRole());
        user.setStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);
        log.debug("invited user persisted id={} email={}", savedUser.getId(), savedUser.getEmail());

        SecurityAccount account = SecurityAccount.builder()
                .user(savedUser).passwordHash(passwordEncoder.encode(request.password()))
                .emailVerified(true)
                .enabled(true)
                .locked(false)
                .build();

        securityAccountRepository.save(account);
        log.debug("security account persisted for invited userId={}", savedUser.getId());

        invitationToken.setUsed(true);
        invitationTokenRepository.save(invitationToken);
        log.debug("invitation token marked as used id={}", invitationToken.getId());

        return issueAuthTokens(savedUser, account, true);
    }

//  ===========
//    FORGOT PASSWORD
//  ===========
    @Override
    public void forgotPassword(String email) {
        String normalizedEmail = normalizeEmail(email);
        log.debug("forgotPassword requested for email={}", normalizedEmail);

        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (userOptional.isEmpty()) {
            log.info("Password reset requested for non-existing email={}", normalizedEmail);
            return;
        }

        User user = userOptional.get();
        SecurityAccount account = getSecurityAccountByUserOrThrow(user);
        log.debug("forgotPassword user/account resolved for userId={}", user.getId());

        if (!account.isEnabled()) {
            log.info("Password reset ignored for disabled account email={}", normalizedEmail);
            return;
        }

        passwordResetTokenRepository.deleteByUser(user);
        log.debug("existing password reset tokens deleted for userId={}", user.getId());

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(Instant.now().plus(PASSWORD_RESET_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        log.debug("new password reset token persisted for userId={} expiresAt={}", user.getId(), resetToken.getExpiryDate());
        emailService.sendPasswordResetEmail(user.getEmail(), token);
        log.debug("password reset email dispatch requested for userId={}", user.getId());

        log.info("Password reset token generated for user {}", user.getEmail());
    }

//  ===========
//    RESET PASSWORD
//  ===========
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        log.debug("resetPassword requested");
        if (!request.newPassword().equals(request.confirmPassword())) {
            log.debug("resetPassword rejected because password confirmation does not match");
            throw new BusinessException("Passwords do not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(request.token())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired token"));

        if (!resetToken.isValid()) {
            log.debug("resetPassword rejected because reset token is invalid");
            throw new InvalidTokenException("Invalid or expired token");
        }

        User user = resetToken.getUser();
        SecurityAccount account = getSecurityAccountByUserOrThrow(user);

        if (passwordEncoder.matches(request.newPassword(), account.getPasswordHash())) {
            log.debug("resetPassword rejected because new password equals current password for userId={}", user.getId());
            throw new BusinessException("New password must be different from the current password");
        }

        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        securityAccountRepository.save(account);
        log.debug("password hash updated for userId={}", user.getId());

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        log.debug("password reset token marked as used id={}", resetToken.getId());

        revokeAllRefreshTokens(user);
        log.debug("all active refresh tokens revoked after password reset for userId={}", user.getId());

        log.info("Password successfully reset for user {}", user.getEmail());
    }

//  ===========
//    LOGOUT
//  ===========
    @Override
    public void logout(String refreshTokenValue) {
        log.debug("logout requested");
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            log.debug("logout ignored because token is blank");
            return;
        }

        refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.debug("logout revoked refresh token id={} userId={}", token.getId(), token.getUser().getId());
        });
    }

//  ===========
//  LOGOUT ALL
//  ===========
    @Override
    public void logoutAll(UUID userId) {
        log.debug("logoutAll requested for userId={}", userId);
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User not found"));
        revokeAllRefreshTokens(user);
        log.debug("logoutAll completed for userId={}", userId);
    }

//  ===========
//  CHANGE PASSWORD
//  ===========
    @Override
    public AuthMeResponse me(SecurityUser securityUser) {
        log.debug("me requested for userId={}", securityUser.getId());
        return new AuthMeResponse(
                securityUser.getId(),
                securityUser.getEmail(),
                securityUser.getRole().name(),
                securityUser.getStatus().name());
    }

//  ===========
//  CREATE INVITATION
//  ===========
    @Override
    public InvitationResponse createInvitation(CreateInvitationRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        log.debug("createInvitation requested for email={} role={}", normalizedEmail, request.role());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.debug("createInvitation rejected because user already exists for email={}", normalizedEmail);
            throw new EmailAlreadyExistException("Email " + normalizedEmail + " is already in use");
        }

        invitationTokenRepository.findTopByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(normalizedEmail)
                .ifPresent(existingInvitation -> {
            if (existingInvitation.isValid()) {
                existingInvitation.setUsed(true);
                invitationTokenRepository.save(existingInvitation);
                log.debug("previous active invitation marked as used id={} for email={}", existingInvitation.getId(), normalizedEmail);
            }
        });

        InvitationToken invitationToken = InvitationToken.builder()
                .token(UUID.randomUUID().toString())
                .email(normalizedEmail)
                .role(request.role())
                .used(false)
                .expiresAt(Instant.now().plus(INVITATION_DAYS, ChronoUnit.DAYS))
                .build();

        InvitationToken savedToken = invitationTokenRepository.save(invitationToken);
        log.debug("new invitation persisted id={} email={} role={}", savedToken.getId(), savedToken.getEmail(), savedToken.getRole());
        emailService.sendInvitationEmail(savedToken.getEmail(), savedToken.getToken());
        log.debug("invitation email dispatch requested for invitationId={}", savedToken.getId());

        log.info("Invitation created for email={} with role={}", savedToken.getEmail(), savedToken.getRole());

        return new InvitationResponse(
                savedToken.getId(),
                savedToken.getEmail(),
                savedToken.getRole().name(),
                savedToken.getToken(),
                savedToken.getExpiresAt(),
                savedToken.isUsed());
    }

//  ===========
//  PRIVATE METHODS
//  ===========
    private void validateAccountCanAuthenticate(User user, SecurityAccount account) {
        log.debug("validateAccountCanAuthenticate userId={} status={} enabled={} emailVerified={} locked={}", user.getId(), user.getStatus(), account.isEnabled(), account.isEmailVerified(), account.isLocked());
        if (account.isLocked() || user.getStatus() == UserStatus.BLOCKED) {
            throw new AccountLockedException("Account is locked");
        }

        if (!account.isEnabled() || user.getStatus() == UserStatus.INACTIVE) {
            throw new UnauthorizedActionException("Account is disabled");
        }

        if (!account.isEmailVerified() || user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            throw new AccountNotVerifiedException("Email verification is required before login");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedActionException("Account is not active");
        }
    }

    private AuthResponse issueAuthTokens(User user, SecurityAccount account, boolean persistRefreshToken) {
        log.debug("issueAuthTokens userId={} persistRefreshToken={}", user.getId(), persistRefreshToken);
        SecurityUser securityUser = new SecurityUser(user, account);

        String accessToken = jwtService.generateAccessToken(securityUser);
        String refreshToken = jwtService.generateRefreshToken(securityUser);

        if (persistRefreshToken) {
            RefreshToken storedRefreshToken = RefreshToken.builder()
                    .token(refreshToken)
                    .user(user)
                    .expiresAt(jwtService.getExpiration(refreshToken))
                    .revoked(false)
                    .build();

            refreshTokenRepository.save(storedRefreshToken);
            log.debug("refresh token persisted for userId={} expiresAt={}", user.getId(), storedRefreshToken.getExpiresAt());
        }

        return AuthResponse.from(user, accessToken, refreshToken);
    }

    private void issueEmailVerificationCode(User user) {
        log.debug("issueEmailVerificationCode for userId={} ttlMinutes={}", user.getId(), emailVerificationCodeTtlMinutes);
        emailVerificationTokenRepository.deleteByUser(user);
        log.debug("previous email verification tokens deleted for userId={}", user.getId());

        String code = generateVerificationCode();

        EmailVerificationToken verificationToken =
                EmailVerificationToken.builder()
                        .user(user).code(code)
                        .expiryDate(Instant.now().plus(emailVerificationCodeTtlMinutes, ChronoUnit.MINUTES))
                        .used(false)
                        .build();

        emailVerificationTokenRepository.save(verificationToken);
        log.debug("email verification token persisted for userId={} expiresAt={}", user.getId(), verificationToken.getExpiryDate());
        emailService.sendEmailVerificationCode(user.getEmail(), code);
        log.debug("email verification dispatch requested for userId={}", user.getId());
    }

    private void revokeAllRefreshTokens(User user) {
        log.debug("revokeAllRefreshTokens requested for userId={}", user.getId());
        refreshTokenRepository.findAllByUserAndRevokedFalse(user)
                .forEach(token -> {
                    token.setRevoked(true);
                    log.debug("refresh token marked revoked id={} userId={}", token.getId(), user.getId());
                });
    }

    private User getUserByEmailOrThrow(String email) {
        log.debug("getUserByEmailOrThrow lookup for email={}", email);
        return userRepository.findByEmailIgnoreCase(email).orElseThrow(
                () -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private SecurityAccount getSecurityAccountByUserOrThrow(User user) {
        log.debug("getSecurityAccountByUserOrThrow lookup for userId={}", user.getId());
        return securityAccountRepository.findByUser(user).orElseThrow(
                () -> new ResourceNotFoundException("Security account not found for user"));
    }

    private String normalizeEmail(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        log.debug("normalizeEmail inputPresent={} output={}", email != null, normalizedEmail);
        return normalizedEmail;
    }

    private String normalizeFullName(String fullName) {
        String normalizedFullName = fullName == null ? "" : fullName.trim().replaceAll("\\s+", " ");
        log.debug("normalizeFullName inputPresent={} output={}", fullName != null, normalizedFullName);
        return normalizedFullName;
    }

    private String generateVerificationCode() {
        int min = (int) Math.pow(10, VERIFICATION_CODE_DIGITS - 1.0);
        int max = (int) Math.pow(10, VERIFICATION_CODE_DIGITS);
        int code = ThreadLocalRandom.current().nextInt(min, max);
        log.debug("generateVerificationCode produced a {}-digit code", VERIFICATION_CODE_DIGITS);
        return String.valueOf(code);
    }
}