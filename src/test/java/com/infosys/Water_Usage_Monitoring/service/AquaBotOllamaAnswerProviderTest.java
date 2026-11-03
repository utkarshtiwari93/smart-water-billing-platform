package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.config.AquaBotOllamaProperties;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AquaBotOllamaAnswerProviderTest {

    private final AtomicReference<String> responseBody = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<Integer> responseStatus = new AtomicReference<>(200);
    private final AtomicLong responseDelayMs = new AtomicLong();
    private HttpServer server;
    private AquaBotOllamaProperties properties;
    private AquaBotOllamaAnswerProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        responseBody.set("{\"message\":{\"content\":\"Namaste! Check for leaks.\"}}");
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/api/chat", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes()));
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

        properties = new AquaBotOllamaProperties();
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        properties.setModel("test-model");
        properties.setConnectTimeoutMs(500);
        properties.setReadTimeoutMs(1000);
        properties.setMaxOutputTokens(400);
        provider = new AquaBotOllamaAnswerProvider(
                HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500)).build(),
                new ObjectMapper(),
                properties);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void answer_postsNonStreamingMultilingualPromptAndReturnsContent() throws Exception {
        AquaBotResidentContext context = new AquaBotResidentContext(
                true,
                List.of(new AquaBotResidentContext.Reading(
                        LocalDate.of(2026, 11, 2), new BigDecimal("1250"), new BigDecimal("90"))),
                List.of(),
                List.of(),
                false,
                false,
                false);
        assertEquals("Namaste! Check for leaks.",
                provider.answer("Paani ka use kaise kam karun?", context));

        JsonNode request = new ObjectMapper().readTree(requestBody.get());
        assertEquals("test-model", request.path("model").asText());
        assertEquals(false, request.path("stream").asBoolean());
        String userPrompt = request.path("messages").get(1).path("content").asText();
        assertTrue(userPrompt.contains("Paani ka use kaise kam karun?"));
        assertTrue(userPrompt.contains("\"meterReading\":1250"));
        String systemPrompt = request.path("messages").get(0).path("content").asText();
        assertTrue(systemPrompt.contains("Hindi"));
        assertTrue(systemPrompt.contains("Hinglish"));
        assertTrue(systemPrompt.contains("Never invent or infer missing values"));
        assertEquals(400, request.path("options").path("num_predict").asInt());
    }

    @Test
    void answer_withAdminContextSendsOnlyApartmentSummaryAndAdminInstructions() throws Exception {
        AquaBotAdminContext context = new AquaBotAdminContext(
                List.of(new AquaBotAdminContext.PeriodSummary(
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 31),
                        "OPEN",
                        false,
                        new BigDecimal("640"),
                        1,
                        new BigDecimal("640"),
                        List.of(new AquaBotAdminContext.HouseholdConsumption("B-4", new BigDecimal("640"))),
                        List.of(new AquaBotAdminContext.HouseholdConsumption("B-4", new BigDecimal("640"))),
                        List.of(new AquaBotAdminContext.HouseholdAllocation(
                                "B-4", "CONSUMPTION", new BigDecimal("8.25"))),
                        new BigDecimal("8"),
                        new BigDecimal("20"),
                        new BigDecimal("8.25"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        0,
                        true,
                        false)),
                List.of(),
                1,
                false,
                false,
                false);

        assertEquals("Namaste! Check for leaks.", provider.answer("Show the open cycle.", context));

        JsonNode request = new ObjectMapper().readTree(requestBody.get());
        String prompt = request.path("messages").get(1).path("content").asText();
        String systemPrompt = request.path("messages").get(0).path("content").asText();
        assertTrue(prompt.contains("Verified administrator records"));
        assertTrue(prompt.contains("\"allocationIsEstimate\":true"));
        assertTrue(prompt.contains("\"openCycleCount\":1"));
        assertTrue(!prompt.contains("resident@example.com"));
        assertTrue(systemPrompt.contains("Payment and overdue status are unavailable"));
        assertTrue(systemPrompt.contains("OPEN cycles are unfinalized cycles"), systemPrompt);
    }

    @Test
    void answer_withoutConfiguredModel_returnsServiceUnavailable() {
        properties.setModel("  ");

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> provider.answer("How do I save water?"));

        assertEquals(AquaBotErrorCode.PROVIDER_CONFIGURATION_MISSING, exception.getErrorCode());
        assertEquals(null, requestBody.get());
    }

    @Test
    void answer_whenOllamaReportsMissingModel_returnsServiceUnavailable() {
        responseStatus.set(404);
        responseBody.set("{\"error\":\"model not found\"}");

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> provider.answer("Explain a leak alert"));

        assertEquals(AquaBotErrorCode.PROVIDER_MODEL_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    void answer_whenOllamaIsUnavailable_returnsServiceUnavailable() {
        int port = server.getAddress().getPort();
        server.stop(0);
        properties.setBaseUrl("http://localhost:" + port);

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> provider.answer("How can I conserve water?"));

        assertEquals(AquaBotErrorCode.PROVIDER_CONNECTION_FAILED, exception.getErrorCode());
    }

    @Test
    void answer_whenOllamaTimesOut_returnsGatewayTimeout() {
        properties.setReadTimeoutMs(50);
        responseDelayMs.set(250);

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> provider.answer("Explain progressive tariffs"));

        assertEquals(AquaBotErrorCode.PROVIDER_TIMEOUT, exception.getErrorCode());
    }

    @Test
    void answer_whenOllamaReturnsMalformedJson_returnsBadGateway() {
        responseBody.set("not-json");

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> provider.answer("How can I conserve water?"));

        assertEquals(AquaBotErrorCode.PROVIDER_INVALID_RESPONSE, exception.getErrorCode());
    }

    @Test
    void answer_whenOllamaReturnsEmptyContent_returnsBadGateway() {
        responseBody.set("{\"message\":{\"content\":\"  \"}}");

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> provider.answer("How can I conserve water?"));

        assertEquals(AquaBotErrorCode.PROVIDER_INVALID_RESPONSE, exception.getErrorCode());
    }

    @Test
    void answer_withInvalidEndpointConfiguration_returnsServiceUnavailable() {
        properties.setBaseUrl("file:///tmp/ollama");

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> provider.answer("How can I conserve water?"));

        assertEquals(AquaBotErrorCode.PROVIDER_CONFIGURATION_MISSING, exception.getErrorCode());
    }
}
