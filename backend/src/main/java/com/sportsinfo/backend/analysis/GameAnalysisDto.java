package com.sportsinfo.backend.analysis;

import java.time.LocalDateTime;
import java.util.List;

import com.sportsinfo.backend.analysis.GeminiClient.Source;

/** 프론트로 내려주는 분석 결과. */
public record GameAnalysisDto(
        String gameId,
        String analysisMarkdown,
        List<Source> sources,
        LocalDateTime createdAt,
        boolean cached
) {
}
