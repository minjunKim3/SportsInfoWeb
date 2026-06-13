package com.sportsinfo.backend.analysis;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

/**
 * 경기별 분석 결과 캐시. 같은 경기를 다시 누르면 무료 quota를 아끼려고 저장본을 재사용한다.
 * gameId를 PK로 써서 경기당 1건만 유지(refresh 시 덮어씀).
 */
@Entity
public class GameAnalysis {

    @Id
    private String gameId;

    @Lob
    private String analysisMarkdown;

    @Lob
    private String sourcesJson;   // 출처 목록을 JSON 문자열로 보관

    private LocalDateTime createdAt;

    protected GameAnalysis() {
    }

    public GameAnalysis(String gameId, String analysisMarkdown, String sourcesJson) {
        this.gameId = gameId;
        this.analysisMarkdown = analysisMarkdown;
        this.sourcesJson = sourcesJson;
        this.createdAt = LocalDateTime.now();
    }

    public String getGameId() { return gameId; }
    public String getAnalysisMarkdown() { return analysisMarkdown; }
    public String getSourcesJson() { return sourcesJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
