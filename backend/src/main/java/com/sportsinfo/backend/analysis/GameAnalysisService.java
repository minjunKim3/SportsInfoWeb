package com.sportsinfo.backend.analysis;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsinfo.backend.analysis.GeminiClient.Source;
import com.sportsinfo.backend.game.Game;
import com.sportsinfo.backend.game.GameRepository;

/**
 * 경기 분석 파이프라인. 캐시가 있으면 재사용(무료 quota 절약), 없거나 refresh면 Gemini 호출.
 */
@Service
public class GameAnalysisService {

    private final GameRepository gameRepository;
    private final GameAnalysisRepository analysisRepository;
    private final GeminiClient geminiClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                return toDto(cached, true);
            }
        }

        GeminiClient.Result result = geminiClient.generate(PromptBuilder.build(game));
        GameAnalysis saved = analysisRepository.save(
                new GameAnalysis(gameId, result.text(), writeSources(result.sources())));
        return toDto(saved, false);
    }

    private GameAnalysisDto toDto(GameAnalysis analysis, boolean cached) {
        return new GameAnalysisDto(
                analysis.getGameId(),
                analysis.getAnalysisMarkdown(),
                readSources(analysis.getSourcesJson()),
                analysis.getCreatedAt(),
                cached);
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
}
