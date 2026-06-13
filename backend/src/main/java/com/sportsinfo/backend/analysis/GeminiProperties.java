package com.sportsinfo.backend.analysis;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml의 gemini.* 설정. API 키는 환경변수로 주입된다. */
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        String baseUrl,
        String model
) {

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
