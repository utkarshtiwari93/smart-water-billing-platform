package com.infosys.Water_Usage_Monitoring.config;

import com.infosys.Water_Usage_Monitoring.service.AquaBotAnswerProvider;
import com.infosys.Water_Usage_Monitoring.service.AquaBotOllamaAnswerProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AquaBotOllamaProperties.class)
public class AquaBotOllamaConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.aquabot", name = "provider", havingValue = "ollama")
    AquaBotAnswerProvider aquaBotOllamaAnswerProvider(
            AquaBotOllamaProperties properties,
            ObjectMapper objectMapper) {
        if (properties.getConnectTimeoutMs() <= 0
                || properties.getReadTimeoutMs() <= 0
                || properties.getMaxOutputTokens() <= 0) {
            throw new IllegalStateException("AquaBot Ollama timeout and output limits must be positive");
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();
        return new AquaBotOllamaAnswerProvider(client, objectMapper, properties);
    }
}
