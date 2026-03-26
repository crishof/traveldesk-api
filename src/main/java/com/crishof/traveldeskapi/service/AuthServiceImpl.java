package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.dto.*;
import com.crishof.traveldeskapi.exception.*;
import com.crishof.traveldeskapi.model.*;
import com.crishof.traveldeskapi.model.agency.Agency;
import com.crishof.traveldeskapi.model.InvitationToken;
import com.crishof.traveldeskapi.model.security.EmailVerificationToken;
import com.crishof.traveldeskapi.model.security.PasswordResetToken;
import com.crishof.traveldeskapi.model.security.RefreshToken;
import com.crishof.traveldeskapi.model.security.SecurityAccount;
import com.crishof.traveldeskapi.repository.*;
import com.crishof.traveldeskapi.security.jwt.JwtService;
import com.crishof.traveldeskapi.security.principal.SecurityUser;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
public class AuthServiceImpl implements AuthService {

    private static final int VERIFICATION_CODE_DIGITS = 6;
    private static final long PASSWORD_RESET_MINUTES = 30;
    private static final long INVITATION_DAYS = 7;

    private final UserRepository userRepository;
    private final AgencyRepository agencyRepository;
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
        String displayAgencyName = sanitizeAgencyName(request.agencyName());
        String normalizedAgencyName = normalizeAgencyName(request.agencyName());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistException("Email " + normalizedEmail + " is already in use");
        }

        if (agencyRepository.findByNormalizedName(normalizedAgencyName).isPresent()) {
            throw new AgencyAlreadyExistException("Agency " + displayAgencyName + " already exists");
        }

        Agency agency = new Agency();
        agency.setName(displayAgencyName);
        agency.setNormalizedName(normalizedAgencyName);

        Agency savedAgency = agencyRepository.save(agency);

        User user = new User();
        user.setFullName(normalizeFullName(request.fullName()));
        user.setEmail(normalizedEmail);
        user.setRole(Role.ADMIN);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setAgency(savedAgency);


        User savedUser = userRepository.save(user);

        SecurityAccount account = SecurityAccount.builder()
                .user(savedUser)
                .passwordHash(passwordEncoder.encode(request.password()))
                .emailVerified(false)
                .enabled(true)
                .locked(false)
                .build();

        securityAccountRepository.save(account);

        issueEmailVerificationCode(savedUser);

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

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                            normalizedEmail, request.password()));
        } catch (DisabledException ex) {
            throw new AccountNotVerifiedException("Email verification is required before login");
        } catch (BadCredentialsException ex) {
            throw new AuthenticationFailedException("Invalid email or password");
        }

        User user = getUserByEmailOrThrow(normalizedEmail);
        SecurityAccount account = getSecurityAccountByUserOrThrow(user);

        validateAccountCanAuthenticate(user, account);

        return issueAuthTokens(user, account, true);
    }

//  ===========
//    REFRESH
//  ===========
    @Override
    public AuthResponse refreshToken(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new InvalidTokenException("Refresh token is required");
        }

        try {
            if (!jwtService.isTokenValid(refreshTokenValue)) {
                throw new InvalidTokenException("Invalid or expired refresh token");
            }

            RefreshToken storedToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue).orElseThrow(
                    () -> new InvalidTokenException("Invalid or expired refresh token"));

            if (!storedToken.isValid()) {
                throw new InvalidTokenException("Invalid or expired refresh token");
            }

            User user = storedToken.getUser();
            SecurityAccount account = getSecurityAccountByUserOrThrow(user);

            validateAccountCanAuthenticate(user, account);

            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);

            return issueAuthTokens(user, account, true);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }
    }

//  ===========
//    VERIFY EMAIL
//  ===========
    @Override
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElseThrow(
                () -> new InvalidTokenException("Invalid or expired verification code"));

        SecurityAccount account = getSecurityAccountByUserOrThrow(user);

        EmailVerificationToken verificationToken =
                emailVerificationTokenRepository.findTopByUserAndCodeAndUsedFalseOrderByCreatedAtDesc(user, request.code())
                        .orElseThrow(() -> new InvalidTokenException("Invalid or expired verification code"));

        if (!verificationToken.isValid()) {
            throw new InvalidTokenException("Invalid or expired verification code");
        }

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);
        emailVerificationTokenRepository.deleteByUser(user);

        account.setEmailVerified(true);
        securityAccountRepository.save(account);

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        return issueAuthTokens(user, account, true);
    }

//  ===========
//    INVITE INFO
//  ===========
    @Override
    public InviteInfoResponse getInviteInfo(String token) {
        InvitationToken invitationToken = getValidInvitationTokenOrThrow(token);
        Agency agency = getInvitationAgencyOrThrow(invitationToken);

        return new InviteInfoResponse(
                invitationToken.getEmail(),
                invitationToken.getRole().name(),
                agency.getId(),
                agency.getName(),
                invitationToken.getExpiresAt(),
                AcceptInviteRequest.PASSWORD_REQUIREMENTS
        );
    }

//  ===========
//    ACCOUNT VERIFICATION
//  ===========
    @Override
    public AuthResponse acceptInvite(AcceptInviteRequest request) {
        InvitationToken invitationToken = getValidInvitationTokenOrThrow(request.token());
        Agency agency = getInvitationAgencyOrThrow(invitationToken);
        String normalizedEmail = invitationToken.getEmail();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistException("Email " + normalizedEmail + " is already in use");
        }

        User user = new User();
        user.setFullName(normalizeFullName(request.fullName()));
        user.setEmail(normalizedEmail);
        user.setRole(invitationToken.getRole());
        user.setStatus(UserStatus.ACTIVE);
        user.setAgency(agency);

        User savedUser = userRepository.save(user);

        SecurityAccount account = SecurityAccount.builder()
                .user(savedUser).passwordHash(passwordEncoder.encode(request.password()))
                .emailVerified(true)
                .enabled(true)
                .locked(false)
                .build();

        securityAccountRepository.save(account);

        invitationToken.setUsed(true);
        invitationTokenRepository.save(invitationToken);

        return issueAuthTokens(savedUser, account, true);
    }

//  ===========
//    FORGOT PASSWORD
//  ===========
    @Override
    public void forgotPassword(String email) {
        String normalizedEmail = normalizeEmail(email);

        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (userOptional.isEmpty()) {
            return;
        }

        User user = userOptional.get();
        SecurityAccount account = getSecurityAccountByUserOrThrow(user);

        if (!account.isEnabled()) {
            return;
        }

        passwordResetTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(Instant.now().plus(PASSWORD_RESET_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

//  ===========
//    RESET PASSWORD
//  ===========
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(request.token())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired token"));

        if (!resetToken.isValid()) {
            throw new InvalidTokenException("Invalid or expired token");
        }

        User user = resetToken.getUser();
        SecurityAccount account = getSecurityAccountByUserOrThrow(user);

        if (passwordEncoder.matches(request.newPassword(), account.getPasswordHash())) {
            throw new BusinessException("New password must be different from the current password");
        }

        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        securityAccountRepository.save(account);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        revokeAllRefreshTokens(user);
    }

//  ===========
//    LOGOUT
//  ===========
    @Override
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

//  ===========
//  LOGOUT ALL
//  ===========
    @Override
    public void logoutAll(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User not found"));
        revokeAllRefreshTokens(user);
    }

//  ===========
//  CHANGE PASSWORD
//  ===========
    @Override
    public AuthMeResponse me(SecurityUser securityUser) {
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
    public InvitationResponse createInvitation(UUID agencyId, UUID invitedByUserId, CreateInvitationRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency not found"));
        User invitedBy = getUserByIdAndAgencyOrThrow(invitedByUserId, agencyId);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistException("Email " + normalizedEmail + " is already in use");
        }

        invitationTokenRepository.findTopByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(normalizedEmail)
                .ifPresent(existingInvitation -> {
            if (existingInvitation.isValid()) {
                existingInvitation.setUsed(true);
                invitationTokenRepository.save(existingInvitation);
            }
        });

        InvitationToken invitationToken = InvitationToken.builder()
                .token(UUID.randomUUID().toString())
                .email(normalizedEmail)
                .agency(agency)
                .invitedBy(invitedBy)
                .role(request.role())
                .used(false)
                .expiresAt(Instant.now().plus(INVITATION_DAYS, ChronoUnit.DAYS))
                .build();

        InvitationToken savedToken = invitationTokenRepository.save(invitationToken);
        emailService.sendInvitationEmail(savedToken.getEmail(), savedToken.getToken());

        return new InvitationResponse(
                savedToken.getId(),
                savedToken.getEmail(),
                savedToken.getRole().name(),
                savedToken.getToken(),
                savedToken.getExpiresAt(),
                savedToken.isUsed());
    }

//  ===========
//  RESEND VERIFICATION CODE
//  ===========
    @Override
    public void resendVerificationCode(String email) {
        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElseThrow(
                () -> new ResourceNotFoundException("User not found"));

        SecurityAccount account = getSecurityAccountByUserOrThrow(user);

        if (account.isEmailVerified() || user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessException("Email is already verified");
        }

        issueEmailVerificationCode(user);
    }

//  ===========
//  PRIVATE METHODS
//  ===========
    private void validateAccountCanAuthenticate(User user, SecurityAccount account) {
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
        }

        return AuthResponse.from(user, accessToken, refreshToken);
    }

    private void issueEmailVerificationCode(User user) {
        emailVerificationTokenRepository.deleteByUser(user);

        String code = generateVerificationCode();

        EmailVerificationToken verificationToken =
                EmailVerificationToken.builder()
                        .user(user).code(code)
                        .expiryDate(Instant.now().plus(emailVerificationCodeTtlMinutes, ChronoUnit.MINUTES))
                        .used(false)
                        .build();

        emailVerificationTokenRepository.save(verificationToken);
        emailService.sendEmailVerificationCode(user.getEmail(), code);
    }

    private void revokeAllRefreshTokens(User user) {
        refreshTokenRepository.findAllByUserAndRevokedFalse(user)
                .forEach(token -> token.setRevoked(true));
    }

    private InvitationToken getValidInvitationTokenOrThrow(String rawToken) {
        InvitationToken invitationToken = invitationTokenRepository.findByToken(rawToken == null ? "" : rawToken.trim())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired invitation token"));

        if (!invitationToken.isValid()) {
            throw new InvalidTokenException("Invalid or expired invitation token");
        }

        return invitationToken;
    }

    private Agency getInvitationAgencyOrThrow(InvitationToken invitationToken) {
        if (invitationToken.getAgency() == null) {
            throw new InvalidTokenException("Invitation token is missing agency context. Request a new invitation");
        }

        return invitationToken.getAgency();
    }

    private User getUserByEmailOrThrow(String email) {
        return userRepository.findByEmailIgnoreCase(email).orElseThrow(
                () -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private User getUserByIdAndAgencyOrThrow(UUID userId, UUID agencyId) {
        return userRepository.findByIdAndAgencyId(userId, agencyId).orElseThrow(
                () -> new ResourceNotFoundException("User not found for the provided agency"));
    }

    private SecurityAccount getSecurityAccountByUserOrThrow(User user) {
        return securityAccountRepository.findByUser(user).orElseThrow(
                () -> new ResourceNotFoundException("Security account not found for user"));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeFullName(String fullName) {
        return fullName == null ? "" : fullName.trim().replaceAll("\\s+", " ");
    }

    private String generateVerificationCode() {
        int min = (int) Math.pow(10, VERIFICATION_CODE_DIGITS - 1.0);
        int max = (int) Math.pow(10, VERIFICATION_CODE_DIGITS);
        int code = ThreadLocalRandom.current().nextInt(min, max);
        return String.valueOf(code);
    }

    private String sanitizeAgencyName(String agencyName) {
        return agencyName.trim().replaceAll("\\s+", " ");
    }

    private String normalizeAgencyName(String agencyName) {
        return sanitizeAgencyName(agencyName).toLowerCase(Locale.ROOT);
    }
}