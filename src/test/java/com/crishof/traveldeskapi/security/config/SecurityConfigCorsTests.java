package com.crishof.traveldeskapi.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.cors.allowed-origins=http://localhost:5173,https://traveldesk-pi.vercel.app/auth/register,https://*.vercel.app"
})
class SecurityConfigCorsTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowPreflightForConfiguredFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/auth/signup")
                        .header("Origin", "https://traveldesk-pi.vercel.app")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://traveldesk-pi.vercel.app"))
                .andExpect(header().string("Vary", org.hamcrest.Matchers.containsString("Origin")));
    }

    @Test
    void shouldRejectPreflightForNonConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/auth/signup")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldKeepCorsHeadersOnPublicSignupRequestFromAllowedOrigin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .header("Origin", "https://preview-123.vercel.app")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://preview-123.vercel.app"));
    }
}


