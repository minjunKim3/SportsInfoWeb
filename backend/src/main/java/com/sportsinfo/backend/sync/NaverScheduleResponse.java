package com.sportsinfo.backend.sync;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 네이버 스포츠 일정 API(/schedule/games)의 JSON 응답을 그대로 옮긴 형태.
 * 비공식 API라 필드가 추가/변경될 수 있으므로 모르는 필드는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverScheduleResponse(
        int code,
        boolean success,
        Result result
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            List<NaverGame> games,
            Integer gameTotalCount
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NaverGame(
            String gameId,
            String superCategoryId,
            String categoryId,
            String categoryName,
            LocalDate gameDate,
            LocalDateTime gameDateTime,
            String stadium,
            String title,
            String homeTeamCode,
            String homeTeamName,
            Integer homeTeamScore,
            String homeTeamEmblemUrl,
            String awayTeamCode,
            String awayTeamName,
            Integer awayTeamScore,
            String awayTeamEmblemUrl,
            String statusCode,
            String statusInfo,
            Boolean cancel,
            Boolean suspended,
            String broadChannel
    ) {
    }
}
