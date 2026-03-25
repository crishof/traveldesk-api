package com.crishof.traveldeskapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrontendUrlPropertiesTests {

    @Test
    void shouldConfigureDevFrontendUrls() throws IOException {
        Properties properties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-dev.properties"));

        assertEquals("${RESET_PASSWORD_BASE_URL:http://localhost:4200/reset-password}",
                properties.getProperty("app.reset-password.base-url"));
        assertEquals("${ACCEPT_INVITE_BASE_URL:http://localhost:4200/accept-invite}",
                properties.getProperty("app.accept-invite.base-url"));
    }

    @Test
    void shouldConfigureProdFrontendUrls() throws IOException {
        Properties properties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-prod.properties"));

        assertEquals("${RESET_PASSWORD_BASE_URL:https://traveldesk-pi.vercel.app/reset-password}",
                properties.getProperty("app.reset-password.base-url"));
        assertEquals("${ACCEPT_INVITE_BASE_URL:https://traveldesk-pi.vercel.app/accept-invite}",
                properties.getProperty("app.accept-invite.base-url"));
    }

    @Test
    void shouldConfigureTestFrontendUrls() throws IOException {
        Properties properties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-test.properties"));

        assertEquals("http://localhost:4200/reset-password", properties.getProperty("app.reset-password.base-url"));
        assertEquals("http://localhost:4200/accept-invite", properties.getProperty("app.accept-invite.base-url"));
    }
}


