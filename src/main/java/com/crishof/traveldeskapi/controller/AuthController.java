package com.crishof.traveldeskapi.controller;

import com.crishof.traveldeskapi.dto.*;
import com.crishof.traveldeskapi.security.principal.SecurityUser;
import com.crishof.traveldeskapi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

//  ===========
//    SIGN UP
//  ===========
    @Operation(summary = "Sign up a new user")
    @ApiResponse(responseCode = "201", description = "User created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("Signup request received for email={}", request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

//  ===========
//    LOGIN
//  ===========
    @Operation(summary = "Login a user")
    @ApiResponse(responseCode = "200", description = "User logged in successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email={}", request.email());
        return ResponseEntity.ok(authService.login(request));
    }

//  ===========
//   INVITE INFO
//  ===========
    @Operation(summary = "Read invitation info", description = "Returns public invitation details required before accepting the invite")
    @ApiResponse(responseCode = "200", description = "Invitation info retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Invalid or expired invitation token")
    @GetMapping("/invite-info/{token}")
    public ResponseEntity<InviteInfoResponse> getInviteInfo(@PathVariable String token) {
        log.info("Invite info request received");
        return ResponseEntity.ok(authService.getInviteInfo(token));
    }

//  ===========
//   ACCEPT INVITE
//  ===========
    @Operation(summary = "Accept a team invitation")
    @ApiResponse(responseCode = "200", description = "Invitation accepted successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/accept-invite")
    public ResponseEntity<AuthResponse> acceptInvite(@Valid @RequestBody AcceptInviteRequest request) {
        log.info("Accept invite request received");
        return ResponseEntity.ok(authService.acceptInvite(request));
    }

//  ===========
//    LOGOUT
//  ===========
    @Operation(summary = "Logout the authenticated user")
    @ApiResponse(responseCode = "200", description = "User logged out successfully")
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }

//  ===========
//    LOGOUT ALL SESSIONS
//  ===========
    @Operation(summary = "Logout from all sessions")
    @ApiResponse(responseCode = "200", description = "User logged out from all sessions successfully")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout-all")
    public ResponseEntity<MessageResponse> logoutAll(@AuthenticationPrincipal SecurityUser user) {
        authService.logoutAll(user.getId());
        return ResponseEntity.ok(new MessageResponse("Logout from all sessions successful"));
    }

//  ===========
//    GET AUTHENTICATED USER DETAILS
//  ===========
    @Operation(summary = "Get authenticated user details")
    @ApiResponse(responseCode = "200", description = "User details retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> me(@AuthenticationPrincipal SecurityUser user) {
        return ResponseEntity.ok(authService.me(user));
    }

//  ===========
//    REFRESH TOKEN
//  ===========
    @Operation(summary = "Refresh JWT token", description = "Generates new access token using refresh token")
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
    @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.refreshToken()));
    }

//  ===========
//    VERIFICATION
//  ===========
    @Operation(summary = "Verify user email", description = "Verifies account email with one-time code")
    @ApiResponse(responseCode = "200", description = "Email verified successfully")
    @ApiResponse(responseCode = "422", description = "Invalid or expired verification code")
    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

//  ===========
//    FORGOT PASSWORD
//  ===========
    @Operation(summary = "Forgot password", description = "Sends password reset link to user email")
    @ApiResponse(responseCode = "200", description = "Reset link sent if email exists")
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(new MessageResponse("If the email exists, a password reset link has been sent"));
    }

//  ===========
//    RESET PASSWORD
//  ===========
    @Operation(summary = "Reset password", description = "Resets password using reset token")
    @ApiResponse(responseCode = "200", description = "Password reset successfully")
    @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Password reset successfully"));
    }
}
