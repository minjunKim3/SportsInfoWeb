package com.sportsinfo.backend.sync;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 수집 주기를 정의한다.
 * - 오늘 경기: 5분마다 (진행 중 스코어/상태 갱신용)
 * - 향후 일주일 일정: 매일 새벽 5시 (편성은 자주 안 바뀌므로 하루 한 번이면 충분)
 * 네이버에 부담을 주지 않도록 주기를 보수적으로 잡았다.
 */
@Component
public class GameSyncScheduler {

    private final GameSyncService gameSyncService;

    public GameSyncScheduler(GameSyncService gameSyncService) {
        this.gameSyncService = gameSyncService;
    }

    /** 앱 시작 5초 후 1회 실행되고, 이후 직전 실행이 끝난 시점부터 5분 간격으로 반복. */
    @Scheduled(initialDelay = 5, fixedDelay = 300, timeUnit = TimeUnit.SECONDS)
    public void syncToday() {
        gameSyncService.syncDate(LocalDate.now());
    }

    @Scheduled(cron = "0 0 5 * * *")
    public void syncWeekAhead() {
        LocalDate today = LocalDate.now();
        for (int dayOffset = 1; dayOffset <= 7; dayOffset++) {
            gameSyncService.syncDate(today.plusDays(dayOffset));
        }
    }
}
