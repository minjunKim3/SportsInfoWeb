package com.sportsinfo.backend.game;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sportsinfo.backend.sync.GameSyncService;

@RestController
@RequestMapping("/api")
public class GameController {

    private final GameRepository gameRepository;
    private final GameSyncService gameSyncService;

    public GameController(GameRepository gameRepository, GameSyncService gameSyncService) {
        this.gameRepository = gameRepository;
        this.gameSyncService = gameSyncService;
    }

    /**
     * 하루치 경기 조회. 기본은 오늘.
     * broadcastOnly=true면 한국어 중계 채널이 잡혀 있는 경기만 내려준다.
     */
    @GetMapping("/games")
    public List<GameDto> getGames(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "false") boolean broadcastOnly) {

        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        return gameRepository.findByGameDateOrderByGameDateTimeAsc(targetDate).stream()
                .filter(game -> !broadcastOnly || game.hasBroadcast())
                .map(GameDto::from)
                .toList();
    }

    /** 수동 수집 트리거. 개발/디버깅용 (스케줄러를 기다리지 않고 즉시 수집). */
    @PostMapping("/sync")
    public Map<String, Object> sync(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        int count = gameSyncService.syncDate(targetDate);
        return Map.of("date", targetDate.toString(), "synced", count);
    }
}
