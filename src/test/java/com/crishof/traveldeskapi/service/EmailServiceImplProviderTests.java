package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.exception.ExternalServiceException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EmailServiceImplProviderTests {

    @Test
    void shouldUseSmtpProviderWhenConfigured() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailServiceImpl service = buildService(mailSender, WebClient.builder(), "smtp", "unused-key");

        service.sendEmailVerificationCode("test@example.com", "123456");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldIncludeConfiguredFrontendInvitationUrlInEmailBody() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailServiceImpl service = buildService(
                mailSender,
                WebClient.builder(),
                "smtp",
                "unused-key",
                "http://localhost:4200/reset-password",
                "http://localhost:4200/accept-invite"
        );

        service.sendInvitationEmail("test@example.com", "token-123");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(messageCaptor.capture());
        assertEquals("You're invited", messageCaptor.getValue().getSubject());
        assertNotNull(messageCaptor.getValue().getText());
        assertTrue(messageCaptor.getValue().getText().contains("http://localhost:4200/accept-invite?token=token-123"));
    }

    @Test
    void shouldIncludeConfiguredFrontendResetUrlInEmailBody() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailServiceImpl service = buildService(
                mailSender,
                WebClient.builder(),
                "smtp",
                "unused-key",
                "https://traveldesk-pi.vercel.app/reset-password",
                "https://traveldesk-pi.vercel.app/accept-invite"
        );

        service.sendPasswordResetEmail("test@example.com", "token-123");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(messageCaptor.capture());
        assertEquals("Password Reset", messageCaptor.getValue().getSubject());
        assertNotNull(messageCaptor.getValue().getText());
        assertTrue(messageCaptor.getValue().getText().contains("https://traveldesk-pi.vercel.app/reset-password?token=token-123"));
    }

    @Test
    void shouldUseBrevoApiWhenConfigured() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();

        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            capturedRequest.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.CREATED).build());
        });

        EmailServiceImpl service = buildService(mailSender, builder, "brevo-api", "brevo-test-key");

        service.sendInvitationEmail("test@example.com", "token-123");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        assertNotNull(capturedRequest.get());
        assertEquals("https://api.brevo.com/v3/smtp/email", capturedRequest.get().url().toString());
        assertEquals("brevo-test-key", capturedRequest.get().headers().getFirst("api-key"));
    }

    @Test
    void shouldFailWhenBrevoProviderHasNoApiKey() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailServiceImpl service = buildService(mailSender, WebClient.builder(), "brevo-api", "");

        assertThrows(ExternalServiceException.class, () -> service.sendPasswordResetEmail("test@example.com", "token-123"));
    }

    private EmailServiceImpl buildService(JavaMailSender mailSender, WebClient.Builder webClientBuilder,
                                          String provider, String brevoApiKey) {
        return buildService(
                mailSender,
                webClientBuilder,
                provider,
                brevoApiKey,
                "https://traveldesk.app/reset-password",
                "https://traveldesk.app/accept-invite"
        );
    }

    private EmailServiceImpl buildService(JavaMailSender mailSender, WebClient.Builder webClientBuilder,
                                          String provider, String brevoApiKey,
                                          String resetPasswordBaseUrl, String acceptInviteBaseUrl) {
        EmailServiceImpl service = new EmailServiceImpl(mailSender, webClientBuilder);
        ReflectionTestUtils.setField(service, "mailEnabled", true);
        ReflectionTestUtils.setField(service, "fromAddress", "no-reply@traveldesk.app");
        ReflectionTestUtils.setField(service, "resetPasswordBaseUrl", resetPasswordBaseUrl);
        ReflectionTestUtils.setField(service, "acceptInviteBaseUrl", acceptInviteBaseUrl);
        ReflectionTestUtils.setField(service, "emailVerificationCodeTtlMinutes", 10L);
        ReflectionTestUtils.setField(service, "mailProvider", provider);
        ReflectionTestUtils.setField(service, "brevoApiBaseUrl", "https://api.brevo.com/v3");
        ReflectionTestUtils.setField(service, "brevoApiKey", brevoApiKey);
        ReflectionTestUtils.setField(service, "brevoTimeoutSeconds", 5L);
        return service;
    }
}

