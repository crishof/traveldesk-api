package com.crishof.traveldeskapi.controller;

import com.crishof.traveldeskapi.dto.AcceptInviteRequest;
import com.crishof.traveldeskapi.model.InvitationToken;
import com.crishof.traveldeskapi.model.security.RefreshToken;
import com.crishof.traveldeskapi.model.Role;
import com.crishof.traveldeskapi.model.security.SecurityAccount;
import com.crishof.traveldeskapi.model.User;
import com.crishof.traveldeskapi.model.UserStatus;
import com.crishof.traveldeskapi.model.agency.Agency;
import com.crishof.traveldeskapi.repository.AgencyRepository;
import com.crishof.traveldeskapi.repository.InvitationTokenRepository;
import com.crishof.traveldeskapi.repository.RefreshTokenRepository;
import com.crishof.traveldeskapi.repository.SecurityAccountRepository;
import com.crishof.traveldeskapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthInvitationFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityAccountRepository securityAccountRepository;

    @Autowired
    private InvitationTokenRepository invitationTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        invitationTokenRepository.deleteAll();
        securityAccountRepository.deleteAll();
        userRepository.deleteAll();
        agencyRepository.deleteAll();
    }

    @Test
    void shouldExposeInviteInfoPubliclyWithoutJwt() throws Exception {
        InvitationToken invitationToken = createInvitationToken("invite-info-token", "invited@example.com");

        mockMvc.perform(get("/api/v1/auth/invite-info/{token}", invitationToken.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("invited@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.agencyId").value(invitationToken.getAgency().getId().toString()))
                .andExpect(jsonPath("$.agencyName").value(invitationToken.getAgency().getName()))
                .andExpect(jsonPath("$.passwordRequirements").value(AcceptInviteRequest.PASSWORD_REQUIREMENTS));
    }

    @Test
    void shouldAcceptInviteWithoutEmailAndAuthenticateUser() throws Exception {
        InvitationToken invitationToken = createInvitationToken("accept-token", "new.user@example.com");

        mockMvc.perform(post("/api/v1/auth/accept-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "%s",
                                  "fullName": "New User",
                                  "password": "StrongPass1!"
                                }
                                """.formatted(invitationToken.getToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new.user@example.com"))
                .andExpect(jsonPath("$.fullName").value("New User"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        User savedUser = userRepository.findByEmailIgnoreCase("new.user@example.com").orElseThrow();
        assertEquals(invitationToken.getAgency().getId(), savedUser.getAgency().getId());
        assertEquals(Role.USER, savedUser.getRole());
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());

        SecurityAccount account = securityAccountRepository.findByUser(savedUser).orElseThrow();
        assertTrue(passwordEncoder.matches("StrongPass1!", account.getPasswordHash()));
        assertTrue(account.isEmailVerified());
        assertTrue(account.isEnabled());
        assertTrue(refreshTokenRepository.findAllByUserAndRevokedFalse(savedUser).stream()
                .map(RefreshToken::getToken)
                .noneMatch(String::isBlank));

        InvitationToken consumedToken = invitationTokenRepository.findByToken("accept-token").orElseThrow();
        assertTrue(consumedToken.isUsed());
    }

    @Test
    void shouldRejectAcceptInviteWhenPasswordDoesNotMeetPolicy() throws Exception {
        InvitationToken invitationToken = createInvitationToken("weak-password-token", "weak@example.com");

        mockMvc.perform(post("/api/v1/auth/accept-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "%s",
                                  "fullName": "Weak Password User",
                                  "password": "weakpass"
                                }
                                """.formatted(invitationToken.getToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Password must contain at least one uppercase letter")));
    }

    private InvitationToken createInvitationToken(String tokenValue, String email) {
        Agency agency = new Agency();
        agency.setName("TravelDesk Test Agency");
        agency.setNormalizedName(("agency-" + tokenValue).toLowerCase());
        Agency savedAgency = agencyRepository.save(agency);

        User inviter = new User();
        inviter.setFullName("Agency Admin");
        inviter.setEmail("admin-" + tokenValue + "@example.com");
        inviter.setRole(Role.ADMIN);
        inviter.setStatus(UserStatus.ACTIVE);
        inviter.setAgency(savedAgency);
        User savedInviter = userRepository.save(inviter);

        InvitationToken invitationToken = InvitationToken.builder()
                .token(tokenValue)
                .email(email)
                .agency(savedAgency)
                .invitedBy(savedInviter)
                .role(Role.USER)
                .used(false)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        InvitationToken savedToken = invitationTokenRepository.save(invitationToken);
        assertNotNull(savedToken.getId());
        return savedToken;
    }
}



