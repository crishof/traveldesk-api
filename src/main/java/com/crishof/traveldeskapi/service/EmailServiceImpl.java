package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final WebClient.Builder webClientBuilder;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@mail.local}")
    private String fromAddress;

    @Value("${app.reset-password.base-url}")
    private String resetPasswordBaseUrl;

    @Value("${app.accept-invite.base-url}")
    private String acceptInviteBaseUrl;

    @Value("${app.email-verification.code-ttl-minutes:10}")
    private long emailVerificationCodeTtlMinutes;

    @Value("${app.mail.provider:smtp}")
    private String mailProvider;

    @Value("${app.mail.brevo.api-url:https://api.brevo.com/v3}")
    private String brevoApiBaseUrl;

    @Value("${app.mail.brevo.api-key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    @Value("${app.mail.brevo.timeout-seconds:15}")
    private long brevoTimeoutSeconds;

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String token) {
        String resetLink = buildResetLink(token);

        if (!mailEnabled) {
            log.info("Mail disabled. Password reset link for {}: {}", recipientEmail, resetLink);
            return;
        }

        sendEmail(recipientEmail, "Password Reset", getResetPasswordBody(resetLink), "password reset email");
    }

    @Override
    public void sendEmailVerificationCode(String recipientEmail, String code) {
        if (!mailEnabled) {
            log.info("Mail disabled. Verification code for {}: {}", recipientEmail, code);
            return;
        }

        sendEmail(recipientEmail, "Email Verification Code", getVerificationBody(code), "email verification code");
    }

    @Override
    public void sendInvitationEmail(String recipientEmail, String token) {
        String inviteLink = buildInviteLink(token);

        if (!mailEnabled) {
            log.info("Mail disabled. Invitation link for {}: {}", recipientEmail, inviteLink);
            return;
        }

        sendEmail(recipientEmail, "You're invited", getInvitationBody(inviteLink), "invitation email");
    }

    private void sendEmail(String recipientEmail, String subject, String body, String emailTypeLabel) {
        try {
            MailProvider provider = MailProvider.from(mailProvider);
            if (provider == MailProvider.BREVO_API) {
                sendWithBrevoApi(recipientEmail, subject, body);
                return;
            }

            mailSender.send(buildSmtpMessage(recipientEmail, subject, body));
        } catch (ExternalServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ExternalServiceException("Failed to send " + emailTypeLabel, ex);
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

    private @NonNull SimpleMailMessage buildSmtpMessage(String recipientEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipientEmail);
        message.setSubject(subject);
        message.setText(body);
        return message;
    }

    private String getVerificationBody(String code) {
        return """
                Your verification code is:
                %s

                This code expires in %d minute%s.
                If you did not create this account, you can ignore this email.
                """.formatted(code, emailVerificationCodeTtlMinutes, emailVerificationCodeTtlMinutes == 1 ? "" : "s");
    }

    private String getResetPasswordBody(String resetLink) {
        return """
                We received a request to reset your password.

                Use the link below to set a new password:
                %s

                This link expires in 30 minutes.
                If you did not request this, you can ignore this email.
                """.formatted(resetLink);
    }

    private String getInvitationBody(String inviteLink) {
        return """
                You have been invited to create your account.

                Use the link below to accept the invitation:
                %s

                This link expires soon. If you were not expecting this invitation, you can ignore this email.
                """.formatted(inviteLink);
    }

    private void sendWithBrevoApi(String recipientEmail, String subject, String body) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            throw new ExternalServiceException("Brevo API key is missing. Set BREVO_API_KEY or app.mail.brevo.api-key");
        }

        BrevoEmailRequest request = new BrevoEmailRequest(
                new BrevoSender(fromAddress),
                List.of(new BrevoRecipient(recipientEmail)),
                subject,
                body
        );

        webClientBuilder
                .baseUrl(brevoApiBaseUrl)
                .defaultHeader("api-key", brevoApiKey)
                .build()
                .post()
                .uri("/smtp/email")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("no body")
                                .map(errorBody -> new ExternalServiceException("Brevo API email send failed: " + errorBody))
                )
                .toBodilessEntity()
                .block(Duration.ofSeconds(brevoTimeoutSeconds));
    }

    private enum MailProvider {
        SMTP,
        BREVO_API;

        private static MailProvider from(String rawValue) {
            if (rawValue == null) {
                return SMTP;
            }

            String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "brevo-api", "brevo_api", "brevoapi", "brevo" -> BREVO_API;
                default -> SMTP;
            };
        }
    }

    private record BrevoEmailRequest(BrevoSender sender, List<BrevoRecipient> to, String subject, String textContent) {
    }

    private record BrevoSender(String email) {
    }

    private record BrevoRecipient(String email) {
    }
}
