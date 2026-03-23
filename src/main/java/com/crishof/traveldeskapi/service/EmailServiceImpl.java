package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@mail.local}")
    private String fromAddress;

    @Value("${app.reset-password.base-url:http://localhost:5173/reset-password}")
    private String resetPasswordBaseUrl;

    @Value("${app.accept-invite.base-url:http://localhost:5173/accept-invite}")
    private String acceptInviteBaseUrl;

    @Value("${app.email-verification.code-ttl-minutes:10}")
    private long emailVerificationCodeTtlMinutes;

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String token) {
        String resetLink = buildResetLink(token);

        if (!mailEnabled) {
            log.info("Mail disabled. Password reset link for {}: {}", recipientEmail, resetLink);
            return;
        }

        try {
            SimpleMailMessage message = getResetPasswordMessage(recipientEmail, resetLink);

            mailSender.send(message);
        } catch (Exception ex) {
            throw new ExternalServiceException("Failed to send password reset email", ex);
        }
    }

    @Override
    public void sendEmailVerificationCode(String recipientEmail, String code) {
        if (!mailEnabled) {
            log.info("Mail disabled. Verification code for {}: {}", recipientEmail, code);
            return;
        }

        try {
            SimpleMailMessage message = getVerificationMessage(recipientEmail, code);
            mailSender.send(message);
        } catch (Exception ex) {
            throw new ExternalServiceException("Failed to send email verification code", ex);
        }
    }

    @Override
    public void sendInvitationEmail(String recipientEmail, String token) {
        String inviteLink = buildInviteLink(token);

        if (!mailEnabled) {
            log.info("Mail disabled. Invitation link for {}: {}", recipientEmail, inviteLink);
            return;
        }

        try {
            SimpleMailMessage message = getInvitationMessage(recipientEmail, inviteLink);
            mailSender.send(message);
        } catch (Exception ex) {
            throw new ExternalServiceException("Failed to send invitation email", ex);
        }
    }

    private String buildResetLink(String token) {
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        return resetPasswordBaseUrl + "?token=" + encodedToken;
    }

    private String buildInviteLink(String token) {
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        return acceptInviteBaseUrl + "?token=" + encodedToken;
    }

    private @NonNull SimpleMailMessage getVerificationMessage(String recipientEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipientEmail);
        message.setSubject("Email Verification Code");
        message.setText("""
                Your verification code is:
                %s

                This code expires in %d minute%s.
                If you did not create this account, you can ignore this email.
                """.formatted(code, emailVerificationCodeTtlMinutes, emailVerificationCodeTtlMinutes == 1 ? "" : "s"));
        return message;
    }

    private @NonNull SimpleMailMessage getResetPasswordMessage(String recipientEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipientEmail);
        message.setSubject("Password Reset");
        message.setText("""
                We received a request to reset your password.

                Use the link below to set a new password:
                %s

                This link expires in 30 minutes.
                If you did not request this, you can ignore this email.
                """.formatted(resetLink));
        return message;
    }

    private @NonNull SimpleMailMessage getInvitationMessage(String recipientEmail, String inviteLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipientEmail);
        message.setSubject("You're invited");
        message.setText("""
                You have been invited to create your account.

                Use the link below to accept the invitation:
                %s

                This link expires soon. If you were not expecting this invitation, you can ignore this email.
                """.formatted(inviteLink));
        return message;
    }
}
