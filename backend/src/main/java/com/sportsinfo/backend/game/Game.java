package com.sportsinfo.backend.game;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.sportsinfo.backend.sync.NaverScheduleResponse.NaverGame;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * 네이버에서 수집한 경기 1건. 네이버의 gameId를 그대로 PK로 사용해서
 * 같은 경기를 다시 받아오면 INSERT가 아니라 UPDATE가 되도록 한다(upsert).
 */
@Entity
@Table(name = "games", indexes = @Index(name = "idx_games_game_date", columnList = "gameDate"))
public class Game {

    @Id
    private String gameId;

    private String superCategoryId;   // baseball, football, ...
    private String categoryId;        // kbo, mlb, epl, ...
    private String categoryName;      // "KBO리그", "메이저리그", ...

    private LocalDate gameDate;
    private LocalDateTime gameDateTime;

    private String stadium;
    private String title;             // 팀 경기가 아닌 특집/기타 콘텐츠의 제목

    private String homeTeamCode;
    private String homeTeamName;
    private Integer homeTeamScore;
    private String homeTeamEmblemUrl;

    private String awayTeamCode;
    private String awayTeamName;
    private Integer awayTeamScore;
    private String awayTeamEmblemUrl;

    private String statusCode;        // BEFORE(예정) / STARTED(진행중) / RESULT(종료)
    private String statusInfo;        // "6회초", "전반 23'" 같은 실시간 상태
    private boolean cancelled;
    private boolean suspended;

    private String broadChannel;      // 한국어 중계 채널 (SPOTV, KBS N SPORTS, ...)

    private LocalDateTime syncedAt;   // 마지막으로 네이버에서 받아온 시각

    protected Game() {
        // JPA 기본 생성자
    }

    public static Game from(NaverGame naverGame) {
        Game game = new Game();
        game.gameId = naverGame.gameId();
        game.applyFrom(naverGame);
        return game;
    }

    /** 네이버 응답 한 건을 엔티티에 반영한다. 신규 저장과 갱신이 같은 코드를 쓴다. */
    public void applyFrom(NaverGame naverGame) {
        this.superCategoryId = naverGame.superCategoryId();
        this.categoryId = naverGame.categoryId();
        this.categoryName = naverGame.categoryName();
        this.gameDate = naverGame.gameDate();
        this.gameDateTime = naverGame.gameDateTime();
        this.stadium = naverGame.stadium();
        this.title = naverGame.title();
        this.homeTeamCode = naverGame.homeTeamCode();
        this.homeTeamName = naverGame.homeTeamName();
        this.homeTeamScore = naverGame.homeTeamScore();
        this.homeTeamEmblemUrl = naverGame.homeTeamEmblemUrl();
        this.awayTeamCode = naverGame.awayTeamCode();
        this.awayTeamName = naverGame.awayTeamName();
        this.awayTeamScore = naverGame.awayTeamScore();
        this.awayTeamEmblemUrl = naverGame.awayTeamEmblemUrl();
        this.statusCode = naverGame.statusCode();
        this.statusInfo = naverGame.statusInfo();
        this.cancelled = Boolean.TRUE.equals(naverGame.cancel());
        this.suspended = Boolean.TRUE.equals(naverGame.suspended());
        this.broadChannel = naverGame.broadChannel();
        this.syncedAt = LocalDateTime.now();
    }

    public boolean hasBroadcast() {
        return broadChannel != null && !broadChannel.isBlank();
    }

    public String getGameId() { return gameId; }
    public String getSuperCategoryId() { return superCategoryId; }
    public String getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public LocalDate getGameDate() { return gameDate; }
    public LocalDateTime getGameDateTime() { return gameDateTime; }
    public String getStadium() { return stadium; }
    public String getTitle() { return title; }
    public String getHomeTeamCode() { return homeTeamCode; }
    public String getHomeTeamName() { return homeTeamName; }
    public Integer getHomeTeamScore() { return homeTeamScore; }
    public String getHomeTeamEmblemUrl() { return homeTeamEmblemUrl; }
    public String getAwayTeamCode() { return awayTeamCode; }
    public String getAwayTeamName() { return awayTeamName; }
    public Integer getAwayTeamScore() { return awayTeamScore; }
    public String getAwayTeamEmblemUrl() { return awayTeamEmblemUrl; }
    public String getStatusCode() { return statusCode; }
    public String getStatusInfo() { return statusInfo; }
    public boolean isCancelled() { return cancelled; }
    public boolean isSuspended() { return suspended; }
    public String getBroadChannel() { return broadChannel; }
    public LocalDateTime getSyncedAt() { return syncedAt; }
}
