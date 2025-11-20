package com.algangi.mongle.chatbot.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "ai.chatbot")
@Getter
@Setter
public class AiChatbotConfig {

    private String baseUrl;
    private int connectTimeoutMillis;
    private int readTimeoutMillis;

    @Bean
    public RestTemplate aiChatbotRestTemplate(RestTemplateBuilder builder) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMillis);

        return builder
            .rootUri(baseUrl)
            .setConnectTimeout(Duration.ofMillis(connectTimeoutMillis))
            .setReadTimeout(Duration.ofMillis(readTimeoutMillis))
            .requestFactory(() -> factory)
            .build();
    }
}
