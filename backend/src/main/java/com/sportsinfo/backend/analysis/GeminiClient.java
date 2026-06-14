package com.sportsinfo.backend.analysis;

import java.time.Duration;
import java.util.List;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Gemini 생성 API 호출 담당. google_search 도구를 켜서 모델이 실제 웹검색을
 * 수행한 뒤 답하도록 한다(grounding). 외부 호출은 전부 이 클래스 뒤로 숨긴다.
 */
@Component
public class GeminiClient {

    private final RestClient restClient;
    private final GeminiProperties properties;

    public GeminiClient(GeminiProperties properties) {
        this.properties = properties;
        // 웹검색 grounding은 수십 초 걸릴 수 있으므로 읽기 타임아웃을 넉넉히.
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }

    /** 프롬프트를 보내고 (분석 텍스트, 출처 URL 목록)을 받는다. */
    public Result generate(String prompt) {
        GeminiRequest request = new GeminiRequest(
                List.of(new Content(List.of(new Part(prompt)))),
                List.of(new Tool(new GoogleSearch())));

        GeminiResponse response;
        try {
            response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/{model}:generateContent")
                            .queryParam("key", properties.apiKey())
                            .build(properties.model()))
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);
        } catch (RestClientResponseException e) {
            // 429: 무료 quota 초과 → 사용자에게 안내 (서버 에러 아님)
            if (e.getStatusCode().value() == 429) {
                throw new QuotaExceededException(
                        "오늘의 무료 분석 한도를 다 썼어요. 잠시 후(또는 내일) 다시 시도해주세요.");
            }
            throw new IllegalStateException("분석 요청이 실패했어요 (Gemini " + e.getStatusCode().value() + ").");
        }

        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return new Result("분석 결과를 받지 못했어요. 잠시 후 다시 시도해주세요.", List.of());
        }
        Candidate candidate = response.candidates().get(0);
        StringBuilder text = new StringBuilder();
        if (candidate.content() != null && candidate.content().parts() != null) {
            for (Part part : candidate.content().parts()) {
                if (part.text() != null) {
                    text.append(part.text());
                }
            }
        }
        return new Result(text.toString(), extractSources(candidate));
    }

    private List<Source> extractSources(Candidate candidate) {
        if (candidate.groundingMetadata() == null
                || candidate.groundingMetadata().groundingChunks() == null) {
            return List.of();
        }
        return candidate.groundingMetadata().groundingChunks().stream()
                .filter(chunk -> chunk.web() != null && chunk.web().uri() != null)
                .map(chunk -> new Source(chunk.web().title(), chunk.web().uri()))
                .toList();
    }

    public record Result(String text, List<Source> sources) {
    }

    public record Source(String title, String uri) {
    }

    // --- 요청/응답 JSON 모델 (Gemini generateContent 스펙) ---

    private record GeminiRequest(List<Content> contents, List<Tool> tools) {
    }

    private record Content(List<Part> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Part(String text) {
    }

    private record Tool(@JsonProperty("google_search") GoogleSearch googleSearch) {
    }

    private record GoogleSearch() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiResponse(List<Candidate> candidates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Candidate(Content content,
                             @JsonProperty("groundingMetadata") GroundingMetadata groundingMetadata) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GroundingMetadata(@JsonProperty("groundingChunks") List<GroundingChunk> groundingChunks) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GroundingChunk(Web web) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Web(String uri, String title) {
    }
}
