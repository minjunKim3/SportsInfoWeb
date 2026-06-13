package com.sportsinfo.backend.game;

import java.time.LocalDateTime;

import com.sportsinfo.backend.broadcast.BroadcastInfo;

/** 프론트엔드에 내려주는 경기 정보. 엔티티를 직접 노출하지 않기 위한 응답 전용 모델. */
public record GameDto(
        String gameId,
        String superCategoryId,
        String categoryId,
        String categoryName,
        LocalDateTime gameDateTime,
        String stadium,
        String title,
        String homeTeamName,
        Integer homeTeamScore,
        String homeTeamEmblemUrl,
        String awayTeamName,
        Integer awayTeamScore,
        String awayTeamEmblemUrl,
        String statusCode,
        String statusInfo,
        boolean cancelled,
        BroadcastInfo broadcast
) {

    public static GameDto from(Game game, BroadcastInfo broadcast) {
        return new GameDto(
                game.getGameId(),
                game.getSuperCategoryId(),
                game.getCategoryId(),
                game.getCategoryName(),
                game.getGameDateTime(),
                game.getStadium(),
                game.getTitle(),
                game.getHomeTeamName(),
                game.getHomeTeamScore(),
                game.getHomeTeamEmblemUrl(),
                game.getAwayTeamName(),
                game.getAwayTeamScore(),
                game.getAwayTeamEmblemUrl(),
                game.getStatusCode(),
                game.getStatusInfo(),
                game.isCancelled(),
                broadcast
        );
    }
}
