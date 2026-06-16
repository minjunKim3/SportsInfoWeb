package com.sportsinfo.backend.analysis;

import java.time.LocalDateTime;
import java.util.List;

import com.sportsinfo.backend.analysis.GeminiClient.Source;

/** 프론트로 내려주는 구조화된 분석 결과. 요약(상단) + 섹션(아코디언). */
public record GameAnalysisDto(
        String gameId,
        boolean meetsThreshold,
        String verdict,
        String expectedViewers,
        List<Section> sections,
        List<Source> sources,
        LocalDateTime createdAt,
        boolean cached
) {

    public record Section(String title, String content) {
    }
}
