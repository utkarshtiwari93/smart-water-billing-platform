package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.config.AquaBotGeminiProperties;
import com.infosys.Water_Usage_Monitoring.exception.AquaBotErrorCode;
import com.infosys.Water_Usage_Monitoring.exception.AquaBotException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AquaBotGeminiAnswerProviderTest {

    private final AtomicReference<String> responseBody = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> apiKeyHeader = new AtomicReference<>();
    private final AtomicReference<Integer> responseStatus = new AtomicReference<>(200);
    private final AtomicLong responseDelayMs = new AtomicLong();
    private HttpServer server;
    private AquaBotGeminiProperties properties;
    private AquaBotGeminiAnswerProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        responseBody.set("""
                {"candidates":[{"content":{"parts":[{"text":"Paani bachane ke liye "},{"text":"leaks check karein."}]}}]}
                """);
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/v1beta/models/test-model:generateContent", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes()));
            apiKeyHeader.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            if (responseDelayMs.get() > 0) {
                try {
                    Thread.sleep(responseDelayMs.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exchange.close();
                    return;
                }
            }
            byte[] body = responseBody.get().getBytes();
            exchange.sendResponseHeaders(responseStatus.get(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        properties = new AquaBotGeminiProperties();
        properties.setApiKey("test-api-key");
        properties.setModel("test-model");
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/v1beta");
        properties.setConnectTimeoutMs(500);
        properties.setReadTimeoutMs(5000);
        properties.setMaxOutputTokens(400);
        provider = new AquaBotGeminiAnswerProvider(
                HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500)).build(),
                new ObjectMapper(),
                properties);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void answer_sendsOfficialGenerateContentRequestAndParsesTextParts() throws Exception {
        AquaBotResidentContext context = new AquaBotResidentContext(
                true,
                List.of(new AquaBotResidentContext.Reading(
                        LocalDate.of(2026, 11, 2), new BigDecimal("1250"), new BigDecimal("90"))),
                List.of(),
                List.of(),
                false,
                false,
                false);
        assertEquals("Paani bachane ke liye leaks check karein.",
                provider.answer("Paani ka use kaise kam karun?", context));

        JsonNode request = new ObjectMapper().readTree(requestBody.get());
        assertEquals("test-api-key", apiKeyHeader.get());
        assertTrue(request.path("systemInstruction").path("parts").get(0).path("text")
                .asText().contains("Hinglish"));
        String prompt = request.path("contents").get(0).path("parts").get(0).path("text").asText();
        assertTrue(prompt.contains("Paani ka use kaise kam karun?"));
        assertTrue(prompt.contains("\"householdAssigned\":true"));
        assertTrue(prompt.contains("\"meterReading\":1250"));
        assertTrue(prompt.contains("\"consumptionLitres\":90"));
        assertTrue(request.path("systemInstruction").path("parts").get(0).path("text")
                .asText().contains("Never invent or infer missing values"));
        assertEquals(400, request.path("generationConfig").path("maxOutputTokens").asInt());
        assertEquals(0.3, request.path("generationConfig").path("temperature").asDouble());
    }

    @Test
    void answer_withAdminContextSendsOnlyApartmentSummaryAndAdminInstructions() throws Exception {
        AquaBotAdminContext context = new AquaBotAdminContext(
                List.of(new AquaBotAdminContext.PeriodSummary(
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 31),
                        "FINALIZED",
                        true,
                        new BigDecimal("900"),
                        1,
                        new BigDecimal("900"),
                        List.of(new AquaBotAdminContext.HouseholdConsumption("A-1", new BigDecimal("900"))),
                        List.of(new AquaBotAdminContext.HouseholdConsumption("A-1", new BigDecimal("900"))),
                        List.of(new AquaBotAdminContext.HouseholdAllocation(
                                "A-1", "CONSUMPTION", new BigDecimal("12.50"))),
                        new BigDecimal("10"),
                        new BigDecimal("25"),
                        new BigDecimal("12.50"),
                        BigDecimal.ZERO,
                        new BigDecimal("30"),
                        1,
                        false,
                        false)),
                List.of(),
                0,
                false,
                false,
                false);

        assertEquals("Paani bachane ke liye leaks check karein.",
                provider.answer("What was our total?", context));

        JsonNode request = new ObjectMapper().readTree(requestBody.get());
        String prompt = request.path("contents").get(0).path("parts").get(0).path("text").asText();
        String systemInstruction = request.path("systemInstruction").path("parts").get(0).path("text").asText();
        assertTrue(prompt.contains("Verified administrator records"));
        assertTrue(prompt.contains("\"flatNumber\":\"A-1\""));
        assertTrue(prompt.contains("\"totalConsumptionLitres\":900"));
        assertTrue(prompt.contains("\"paymentStatusAvailable\":false"));
        assertTrue(!prompt.contains("resident@example.com"));
        assertTrue(systemInstruction.contains("Payment and overdue status are unavailable"));
        assertTrue(systemInstruction.contains("OPEN cycles are"));
    }

    @Test
    void answer_withoutApiKey_returnsServiceUnavailableWithoutRequest() {
        properties.setApiKey(" ");

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> provider.answer("How do I save water?"));

        assertEquals(AquaBotErrorCode.PROVIDER_CONFIGURATION_MISSING, exception.getErrorCode());
        assertNull(requestBody.get());
    }

    @Test
    void answer_withoutModel_returnsServiceUnavailableWithoutRequest() {
        properties.setModel("");

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> provider.answer("How do I save water?"));

        assertEquals(AquaBotErrorCode.PROVIDER_CONFIGURATION_MISSING, exception.getErrorCode());
        assertNull(requestBody.get());
    }

    @Test
    void answer_withInvalidModelPath_returnsServiceUnavailable() {
        properties.setModel("../other");

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> provider.answer("Explain a leak alert"));

        assertEquals(AquaBotErrorCode.PROVIDER_MODEL_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    void answer_whenGeminiIsUnavailable_returnsServiceUnavailable() {
        int port = server.getAddress().getPort();
        server.stop(0);
        properties.setBaseUrl("http://localhost:" + port + "/v1beta");

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> provider.answer("How can I conserve water?"));

        assertEquals(AquaBotErrorCode.PROVIDER_CONNECTION_FAILED, exception.getErrorCode());
    }

    @Test
    void answer_whenGeminiTimesOut_returnsGatewayTimeout() {
        properties.setReadTimeoutMs(50);
        responseDelayMs.set(250);

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> provider.answer("Explain progressive tariffs"));

        assertEquals(AquaBotErrorCode.PROVIDER_TIMEOUT, exception.getErrorCode());
    }

    @Test
    void answer_whenGeminiRejectsApiKeyOrModel_returnsServiceUnavailable() {
        responseStatus.set(403);
        responseBody.set("{\"error\":{\"message\":\"sensitive provider details\"}}");

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> provider.answer("How can I conserve water?"));

        assertEquals(AquaBotErrorCode.PROVIDER_AUTHENTICATION_FAILED, exception.getErrorCode());
        assertTrue(!exception.getMessage().contains("sensitive provider details"));
    }

    @Test
    void answer_whenGeminiReportsInvalidApiKey_returnsAuthenticationFailure() {
        responseStatus.set(400);
        responseBody.set("{\"error\":{\"status\":\"INVALID_ARGUMENT\","
                + "\"message\":\"API key not valid. sensitive provider details\"}}");

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> provider.answer("How can I conserve water?"));

        assertEquals(AquaBotErrorCode.PROVIDER_AUTHENTICATION_FAILED, exception.getErrorCode());
        assertTrue(!exception.getMessage().contains("sensitive provider details"));
    }

    @Test
    void answer_whenGeminiReportsUnsupportedModel_returnsModelUnavailable() {
        responseStatus.set(404);
        responseBody.set("{\"error\":{\"message\":\"model not found: private-model\"}}");

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> provider.answer("How can I conserve water?"));

        assertEquals(AquaBotErrorCode.PROVIDER_MODEL_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    void answer_whenGeminiRateLimits_returnsTooManyRequests() {
        responseStatus.set(429);
        responseBody.set("{\"error\":{\"message\":\"quota exceeded\"}}");

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> provider.answer("How can I conserve water?"));

        assertEquals(AquaBotErrorCode.PROVIDER_RATE_LIMITED, exception.getErrorCode());
    }

    @Test
    void answer_whenGeminiHasUpstreamError_returnsUpstreamUnavailable() {
        responseStatus.set(502);
        responseBody.set("{\"error\":{\"message\":\"sensitive provider details\"}}");

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> provider.answer("How can I conserve water?"));

        assertEquals(AquaBotErrorCode.PROVIDER_UPSTREAM_UNAVAILABLE, exception.getErrorCode());
        assertTrue(!exception.getMessage().contains("sensitive provider details"));
    }

    @Test
    void answer_whenGeminiReturnsMalformedJson_returnsBadGateway() {
        responseBody.set("not-json");

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> provider.answer("How can I conserve water?"));

        assertEquals(AquaBotErrorCode.PROVIDER_INVALID_RESPONSE, exception.getErrorCode());
    }

    @Test
    void answer_whenGeminiReturnsNoCandidateText_returnsBadGateway() {
        responseBody.set("{\"candidates\":[{\"content\":{\"parts\":[]}}]}");

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> provider.answer("How can I conserve water?"));

        assertEquals(AquaBotErrorCode.PROVIDER_INVALID_RESPONSE, exception.getErrorCode());
    }
}
