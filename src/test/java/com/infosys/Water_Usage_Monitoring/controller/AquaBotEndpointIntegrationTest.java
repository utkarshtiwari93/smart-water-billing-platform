package com.infosys.Water_Usage_Monitoring.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.aquabot.enabled=false")
class AquaBotEndpointIntegrationTest {

    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void chat_withoutAuthentication_returnsUnauthorized() throws Exception {
        mvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"How can I save water?\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void chat_whenFeatureFlagDisabled_isUnavailableEvenToAuthenticatedUsers() throws Exception {
        mvc.perform(post("/api/v1/assistant/chat")
                        .with(user("resident").roles("RESIDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"How can I save water?\"}"))
                .andExpect(status().isNotFound());
    }
}
