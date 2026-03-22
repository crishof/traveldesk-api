package com.crishof.traveldeskapi.service;

public interface EmailService {

    void sendPasswordResetEmail(String recipientEmail, String token);

    void sendEmailVerificationCode(String recipientEmail, String code);

    void sendInvitationEmail(String recipientEmail, String token);
}
