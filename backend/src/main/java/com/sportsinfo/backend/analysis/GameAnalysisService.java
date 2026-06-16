package com.sportsinfo.backend.analysis;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsinfo.backend.analysis.GameAnalysisDto.Section;
import com.sportsinfo.backend.analysis.GeminiClient.Source;
import com.sportsinfo.backend.game.Game;
import com.sportsinfo.backend.game.GameRepository;

/**
 * 경기 분석 파이프라인. 캐시가 있으면 재사용(무료 quota 절약), 없거나 refresh면 Gemini 호출.
 * Gemini가 돌려준 JSON을 구조화된 DTO(요약 + 섹션)로 파싱한다.
 */
@Service
public class GameAnalysisService {

    private final GameRepository gameRepository;
    private final GameAnalysisRepository analysisRepository;
    private final GeminiClient geminiClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public GameAnalysisService(GameRepository gameRepository,
                               GameAnalysisRepository analysisRepository,
                               GeminiClient geminiClient,
                               GeminiProperties geminiProperties) {
        this.gameRepository = gameRepository;
        this.analysisRepository = analysisRepository;
        this.geminiClient = geminiClient;
        this.geminiProperties = geminiProperties;
    }

    public GameAnalysisDto analyze(String gameId, boolean refresh) {
        if (!geminiProperties.isConfigured()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY가 설정되지 않았어요. 환경변수에 키를 넣고 서버를 재시작해주세요.");
        }

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("해당 경기를 찾을 수 없어요: " + gameId));

        if (!refresh) {
            GameAnalysis cached = analysisRepository.findById(gameId).orElse(null);
            if (cached != null) {
                return toDto(cached.getGameId(), cached.getAnalysisMarkdown(),
                        readSources(cached.getSourcesJson()), cached.getCreatedAt(), true);
            }
        }

        GeminiClient.Result result = geminiClient.generate(PromptBuilder.build(game));
        GameAnalysis saved = analysisRepository.save(
                new GameAnalysis(gameId, result.text(), writeSources(result.sources())));
        return toDto(saved.getGameId(), saved.getAnalysisMarkdown(),
                result.sources(), saved.getCreatedAt(), false);
    }

    /** Gemini가 준 JSON 텍스트를 파싱해 DTO로. 실패하면 원문을 한 섹션으로 보여주는 안전 폴백. */
    private GameAnalysisDto toDto(String gameId, String rawText, List<Source> sources,
                                  java.time.LocalDateTime createdAt, boolean cached) {
        try {
            ParsedAnalysis p = objectMapper.readValue(extractJson(rawText), ParsedAnalysis.class);
            return new GameAnalysisDto(
                    gameId,
                    Boolean.TRUE.equals(p.meetsThreshold()),
                    nullToEmpty(p.verdict()),
                    nullToEmpty(p.expectedViewers()),
                    p.sections() != null ? p.sections() : List.of(),
                    sources, createdAt, cached);
        } catch (Exception e) {
            // 파싱 실패(옛 형식/형식 오류) → 원문을 그대로 한 섹션에 담아 보여준다.
            return new GameAnalysisDto(gameId, false, "분석 결과", "",
                    List.of(new Section("분석", rawText)), sources, createdAt, cached);
        }
    }

    /** 응답에 코드펜스나 잡텍스트가 섞여도 첫 '{' ~ 마지막 '}'만 잘라 JSON으로 본다. */
    private String extractJson(String text) {
        if (text == null) {
            return "{}";
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return (start >= 0 && end > start) ? text.substring(start, end + 1) : text;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String writeSources(List<Source> sources) {
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<Source> readSources(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Source>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ParsedAnalysis(
            Boolean meetsThreshold,
            String verdict,
            String expectedViewers,
            List<Section> sections
    ) {
    }
}
