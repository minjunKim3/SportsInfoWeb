package com.sportsinfo.backend.analysis;

import java.time.format.DateTimeFormatter;

import com.sportsinfo.backend.game.Game;

/**
 * 경기 1건을 "볼 가치 분석" 프롬프트로 변환한다.
 * 결과를 화면에서 요약+아코디언으로 보여주기 위해, 자유 텍스트가 아니라
 * 정해진 JSON 구조로 답하게 한다.
 *  - meetsThreshold: 한국어 중계 경기 전체 표본에서 상위 55% 이상인지(true/false)
 *  - verdict: 한 줄 요약
 *  - expectedViewers: 전세계 예상 시청자 수
 *  - sections: 항목별 상세(클릭해서 펼치는 용도)
 * 반드시 웹검색으로 최신 정보를 수집해 판단한다.
 */
final class PromptBuilder {

    private static final DateTimeFormatter KOREAN_DATETIME =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm");

    private PromptBuilder() {
    }

    static String build(Game game) {
        String matchup = (game.getHomeTeamName() != null && game.getAwayTeamName() != null)
                ? game.getAwayTeamName() + " vs " + game.getHomeTeamName()
                : (game.getTitle() != null ? game.getTitle() : "(팀 정보 없음)");
        String when = game.getGameDateTime() != null
                ? game.getGameDateTime().format(KOREAN_DATETIME)
                : "미정";
        String score = (game.getHomeTeamScore() != null && game.getAwayTeamScore() != null)
                ? (game.getAwayTeamName() + " " + game.getAwayTeamScore()
                   + " : " + game.getHomeTeamScore() + " " + game.getHomeTeamName())
                : "경기 전";

        return """
                너는 스포츠 경기의 '볼 가치'를 분석하는 전문가다.

                ## 분석 대상 경기 (우리 서비스 DB 정보)
                - 종목/리그: %s
                - 대진: %s
                - 일시: %s
                - 현재 상태: %s
                - 스코어: %s

                ## 매우 중요한 규칙
                반드시 **구글 웹검색을 사용**해 이 경기의 실제 최신 정보(기사, 순위표, 기록,
                스타 선수 출전 여부 등)를 수집한 뒤 분석하라. 내부 지식만으로 추측하지 마라.
                위 경기가 분석 대상이다 — 다른 경기를 찾지 말 것.

                ## 판단 기준
                표본 = '한국어 중계가 존재하는 모든 스포츠 및 e스포츠 경기'.
                이 표본에서 이 경기가 **상위 55%% 이상의 볼 가치**가 있는지 판단하라.
                고려 요소: 두 팀/선수의 순위·위치(리그면 순위·게임차·승점 명시), 경기 결과의
                파급, 화제성, 가비지타임 여부, 대회 위상, 팀 인기, 시즌 단계(개막/막바지/특수
                시점), 스타 선수 출전·결장, 기록 도전(연승·신기록 등), 신인/컴백/은퇴 스토리,
                이적 스토리, 시리즈 내 위치, 토너먼트 라운드·단판/다전제·탈락 직결·업셋 여부.

                ## 출력 형식 (반드시 이 JSON만 출력, 마크다운 코드펜스/설명 금지)
                {
                  "meetsThreshold": true 또는 false,
                  "verdict": "한 줄 요약 (예: 순위 직결 빅매치, 놓치면 아쉬움)",
                  "expectedViewers": "전세계 기준 예상 시청자 수 (현장+TV+온라인 합산, 한국 아님). 근거 한 줄 포함",
                  "sections": [
                    {"title": "팀 순위 · 현재 위치", "content": "리그 순위/게임차/승점 등 구체적으로"},
                    {"title": "이 경기의 의미", "content": "결과에 따라 상황이 어떻게 전개되는지"},
                    {"title": "화제성 · 대회 위상", "content": "팬들에게 얼마나 빅뉴스인지, 리그 위상"},
                    {"title": "스타 선수 · 기록 · 스토리", "content": "출전/결장, 기록 도전, 이적/복귀 스토리 등 해당되는 것"},
                    {"title": "누가 더 간절한가", "content": "더 간절한 팀/선수를 명확히 지목하고 이유를. 응원팀 고르기용"}
                  ]
                }

                각 content는 한국어로 구체적이고 충실하게. 해당 없는 섹션은 빼도 된다.
                meetsThreshold가 false여도 expectedViewers와 sections는 채워라.
                """.formatted(
                game.getCategoryName(),
                matchup,
                when,
                statusText(game.getStatusCode()),
                score);
    }

    private static String statusText(String statusCode) {
        if (statusCode == null) {
            return "미정";
        }
        return switch (statusCode) {
            case "BEFORE" -> "경기 예정";
            case "STARTED" -> "경기 진행 중";
            case "RESULT" -> "경기 종료";
            default -> statusCode;
        };
    }
}
