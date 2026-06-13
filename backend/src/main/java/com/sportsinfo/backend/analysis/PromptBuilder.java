package com.sportsinfo.backend.analysis;

import java.time.format.DateTimeFormatter;

import com.sportsinfo.backend.game.Game;

/**
 * 경기 1건을 "볼 가치 분석" 프롬프트로 변환한다.
 * 사용자 요구의 핵심을 담았다:
 *  - 표본 = 한국어 중계가 있는 모든 스포츠/e스포츠 경기
 *  - 이 경기가 상위 55%인지 판정 (여러 요소 고려)
 *  - 상위면 전세계 예상 시청자 수
 *  - 누가 더 간절한지 + 이유 (응원팀 고르기용)
 *  - 반드시 웹검색으로 최신 정보를 수집해 판단
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
                - 중계 채널: %s

                ## 매우 중요한 규칙
                반드시 **구글 웹검색을 사용**해서 이 경기에 대한 실제 최신 정보(기사, 순위표, 기록,
                스타 선수 출전 여부 등)를 수집한 뒤 분석하라. 절대 내부 지식만으로 추측하지 마라.
                위 경기 정보가 화제의 경기다 — 다른 경기를 찾지 말고 이 경기를 분석하라.

                ## 표본과 기준
                표본 = '한국어 중계가 존재하는 모든 스포츠 및 e스포츠 경기'. 이 표본 안에서
                이 경기가 **상위 55%% 이상의 볼 가치**가 있는지 판정하라. 다음 요소들을 고려한다:
                1. 두 팀/선수의 리그·대회 내 현재 순위/위치 (리그면 순위·게임차·승점을 꼭 텍스트로 명시)
                2. 이 경기 결과에 따라 상황이 어떻게 전개되는지
                3. 팬·대중에게 얼마나 빅뉴스인지
                4. 스코어가 가비지타임(승부 갈림)인지
                5. 대회/리그의 위상
                6. 두 팀의 인기
                7. 시즌 단계 (개막전/시즌 막바지/올스타 직전 등 특수 시점)
                8. 스타 플레이어 출전/결장
                9. 진행 중인 기록 도전 (연승, 신기록, MVP 경쟁 등)
                10. 신인 데뷔/컴백/은퇴/영구결번 등 스토리
                11. 직전 트레이드·이적으로 인한 스토리 (전 소속팀 상대 등)
                12. 시리즈 내 위치 (PO 1차전 vs 7차전 등)
                13. 토너먼트 라운드(8강<4강<결승), 단판/다전제, 탈락 직결(엘리미네이션), 업셋 매치업 여부

                ## 출력 형식 (한국어 마크다운)
                ### 1. 상위 몇 %% 인가
                - 결론: 상위 약 N%% (그리고 상위 55%% 기준 충족 여부 ✅/❌)
                - 근거: 위 요소들 중 실제로 해당하는 것을 웹검색 결과와 함께 구체적으로 서술

                ### 2. 예상 시청자 수 (상위 55%% 이상일 때만 작성)
                - **전세계 기준** 예상 시청자 수 (현장 + 온라인 + TV 전부 포함). 대한민국 기준 아님!
                - 추정 근거를 간단히

                ### 3. 누가 더 간절한가
                - 더 간절한 팀/선수와 그 이유를 구체적으로. 판단 근거 예시:
                  리그/대회 구조상 위치, 이 경기 승패로 순위·포지션이 어떻게 바뀌는지,
                  과거 성적과 현재 순위를 어떻게 받아들일지, 승패로 유지·경신되는 기록이 있는지 등.
                - 응원팀을 고르는 재미를 위한 것이니 명확하게 한쪽을 지목하라.

                분석은 꼼꼼하고 자세하게. 시간이 걸려도 괜찮다.
                """.formatted(
                game.getCategoryName(),
                matchup,
                when,
                statusText(game.getStatusCode()),
                score,
                "(중계 정보는 서비스가 별도 제공)");
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
