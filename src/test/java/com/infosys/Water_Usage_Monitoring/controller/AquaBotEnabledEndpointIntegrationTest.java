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
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AquaBotEnabledEndpointIntegrationTest {

    private static final HttpServer OLLAMA_SERVER = startOllamaServer();

    @Autowired WebApplicationContext context;
    @Autowired ApartmentRepository apartmentRepository;
    @Autowired HouseholdRepository householdRepository;
    @Autowired UserRepository userRepository;
    private MockMvc mvc;
    private String residentEmail;

    @DynamicPropertySource
    static void configureAquaBot(DynamicPropertyRegistry registry) {
        registry.add("app.aquabot.enabled", () -> true);
        registry.add("app.aquabot.provider", () -> "ollama");
        registry.add("app.aquabot.ollama.base-url",
                () -> "http://localhost:" + OLLAMA_SERVER.getAddress().getPort());
        registry.add("app.aquabot.ollama.model", () -> "integration-test-model");
        registry.add("app.aquabot.ollama.connect-timeout-ms", () -> 1000);
        registry.add("app.aquabot.ollama.read-timeout-ms", () -> 1000);
        registry.add("app.aquabot.ollama.max-output-tokens", () -> 100);
    }

    private static HttpServer startOllamaServer() {
        try {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/api/chat", exchange -> {
                exchange.getRequestBody().readAllBytes();
                byte[] response = "{\"message\":{\"content\":\"Hindi and Hinglish supported.\"}}"
                        .getBytes();
                exchange.sendResponseHeaders(200, response.length);
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
    static void stopOllamaServer() {
        OLLAMA_SERVER.stop(0);
    }

    @BeforeEach
    void setUp() {
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
    void chat_withAuthenticatedResident_returnsExistingResponseContract() throws Exception {
        mvc.perform(post("/api/v1/assistant/chat")
                        .with(user(residentEmail).roles("RESIDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Paani ka use kaise kam karun?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Hindi and Hinglish supported."));
    }

    private String createResident() {
        String suffix = UUID.randomUUID().toString();
        Apartment apartment = new Apartment();
        apartment.setName("AquaBot Ollama Test " + suffix);
        Apartment savedApartment = apartmentRepository.save(apartment);

        Household household = new Household();
        household.setApartment(savedApartment);
        household.setFlatNumber("O-" + suffix.substring(0, 8));
        Household savedHousehold = householdRepository.save(household);

        User resident = new User();
        resident.setApartment(savedApartment);
        resident.setHousehold(savedHousehold);
        resident.setName("AquaBot Ollama Test Resident");
        resident.setEmail("aquabot-ollama-" + suffix + "@example.test");
        resident.setRole(Role.RESIDENT);
        userRepository.save(resident);
        return resident.getEmail();
    }
}
