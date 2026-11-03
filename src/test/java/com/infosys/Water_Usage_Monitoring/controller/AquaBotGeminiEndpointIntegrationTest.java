package com.infosys.Water_Usage_Monitoring.controller;

import com.sun.net.httpserver.HttpServer;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.repository.ApartmentRepository;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AquaBotGeminiEndpointIntegrationTest {

    private static final AtomicReference<String> requestApiKey = new AtomicReference<>();
    private static final AtomicReference<String> requestBody = new AtomicReference<>();
    private static final AtomicReference<String> responseBody = new AtomicReference<>();
    private static final AtomicInteger responseStatus = new AtomicInteger(200);
    private static final HttpServer GEMINI_SERVER = startGeminiServer();

    @Autowired WebApplicationContext context;
    @Autowired ApartmentRepository apartmentRepository;
    @Autowired HouseholdRepository householdRepository;
    @Autowired UserRepository userRepository;
    private MockMvc mvc;
    private String residentEmail;
    private String adminEmail;

    @DynamicPropertySource
    static void configureGemini(DynamicPropertyRegistry registry) {
        registry.add("app.aquabot.enabled", () -> true);
        registry.add("app.aquabot.provider", () -> "gemini");
        registry.add("app.aquabot.gemini.api-key", () -> "integration-test-key");
        registry.add("app.aquabot.gemini.model", () -> "test-model");
        registry.add("app.aquabot.gemini.base-url",
                () -> "http://localhost:" + GEMINI_SERVER.getAddress().getPort() + "/v1beta");
        registry.add("app.aquabot.gemini.connect-timeout-ms", () -> 1000);
        registry.add("app.aquabot.gemini.read-timeout-ms", () -> 1000);
        registry.add("app.aquabot.gemini.max-output-tokens", () -> 100);
    }

    private static HttpServer startGeminiServer() {
        try {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/v1beta/models/test-model:generateContent", exchange -> {
                requestApiKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
                requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] response = responseBody.get().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(responseStatus.get(), response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @AfterAll
    static void stopGeminiServer() {
        GEMINI_SERVER.stop(0);
    }

    @BeforeEach
    void setUp() {
        requestApiKey.set(null);
        requestBody.set(null);
        responseStatus.set(200);
        responseBody.set("""
                        {"candidates":[{"content":{"parts":[{"text":"Hindi and Hinglish water tips."}]}}]}
                        """);
        residentEmail = createResident();
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
    void chat_withInvalidMessage_returnsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/assistant/chat")
                        .with(user(residentEmail).roles("RESIDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_withAuthenticatedResidentKeepsExistingResponseAndUsesBackendKey() throws Exception {
        mvc.perform(post("/api/v1/assistant/chat")
                        .with(user(residentEmail).roles("RESIDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Paani ka use kaise kam karun?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Hindi and Hinglish water tips."));

        assertEquals("integration-test-key", requestApiKey.get());
        org.junit.jupiter.api.Assertions.assertTrue(
                requestBody.get().contains("\\\"householdAssigned\\\":true"), requestBody.get());
    }

    @Test
    void chat_whenGeminiRejectsCredentials_returnsSafeTypedError() throws Exception {
        responseStatus.set(403);
        responseBody.set("{\"error\":{\"message\":\"sensitive provider response and key\"}}");

        mvc.perform(post("/api/v1/assistant/chat")
                        .with(user(residentEmail).roles("RESIDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"How can I save water?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value("PROVIDER_AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value(
                        "AquaBot could not authenticate with its AI provider. Contact the administrator."))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("integration-test-key"))))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("sensitive provider response"))));
    }

    @Test
    void chat_whenGeminiRateLimits_returnsTooManyRequests() throws Exception {
        responseStatus.set(429);

        mvc.perform(post("/api/v1/assistant/chat")
                        .with(user(residentEmail).roles("RESIDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"How can I save water?\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("PROVIDER_RATE_LIMITED"));
    }

    @Test
    void chat_asAdmin_isForbidden() throws Exception {
        mvc.perform(post("/api/v1/assistant/chat")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Show household usage\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminChat_withPersistedAdminUsesAdminContextAndExistingResponseContract() throws Exception {
        adminEmail = createAdmin();

        mvc.perform(post("/api/v1/assistant/admin/chat")
                        .with(user(adminEmail).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Show my apartment water summary\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Hindi and Hinglish water tips."));

        org.junit.jupiter.api.Assertions.assertTrue(
                requestBody.get().contains("Verified administrator records"));
        org.junit.jupiter.api.Assertions.assertTrue(
                requestBody.get().contains("\\\"paymentStatusAvailable\\\":false"));
    }

    @Test
    void adminChat_residentCannotAccessEvenWithAdminAuthorityOverride() throws Exception {
        mvc.perform(post("/api/v1/assistant/admin/chat")
                        .with(user(residentEmail).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Show apartment water summary\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminChat_withoutAuthentication_returnsUnauthorized() throws Exception {
        mvc.perform(post("/api/v1/assistant/admin/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Show apartment water summary\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String createResident() {
        String suffix = UUID.randomUUID().toString();
        Apartment apartment = new Apartment();
        apartment.setName("AquaBot Test " + suffix);
        Apartment savedApartment = apartmentRepository.save(apartment);

        Household household = new Household();
        household.setApartment(savedApartment);
        household.setFlatNumber("A-" + suffix.substring(0, 8));
        Household savedHousehold = householdRepository.save(household);

        User resident = new User();
        resident.setApartment(savedApartment);
        resident.setHousehold(savedHousehold);
        resident.setName("AquaBot Test Resident");
        resident.setEmail("aquabot-" + suffix + "@example.test");
        resident.setRole(Role.RESIDENT);
        userRepository.save(resident);
        return resident.getEmail();
    }

    private String createAdmin() {
        String suffix = UUID.randomUUID().toString();
        Apartment apartment = new Apartment();
        apartment.setName("AquaBot Admin Test " + suffix);
        Apartment savedApartment = apartmentRepository.save(apartment);

        User admin = new User();
        admin.setApartment(savedApartment);
        admin.setName("AquaBot Test Admin");
        admin.setEmail("aquabot-admin-" + suffix + "@example.test");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        return admin.getEmail();
    }
}
