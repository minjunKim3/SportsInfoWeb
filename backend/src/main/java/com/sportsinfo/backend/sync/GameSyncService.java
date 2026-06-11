package com.sportsinfo.backend.sync;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sportsinfo.backend.game.Game;
import com.sportsinfo.backend.game.GameRepository;
import com.sportsinfo.backend.sync.NaverScheduleResponse.NaverGame;

/**
 * 수집 파이프라인의 중심. 네이버에서 받아온 경기를 우리 DB에 upsert한다.
 * 한 종목이 실패해도 나머지 종목 수집은 계속되도록 종목 단위로 예외를 격리한다.
 */
@Service
public class GameSyncService {

    private static final Logger log = LoggerFactory.getLogger(GameSyncService.class);

    private final NaverSportsClient naverSportsClient;
    private final GameRepository gameRepository;
    private final NaverSportsProperties properties;

    public GameSyncService(NaverSportsClient naverSportsClient,
                           GameRepository gameRepository,
                           NaverSportsProperties properties) {
        this.naverSportsClient = naverSportsClient;
        this.gameRepository = gameRepository;
        this.properties = properties;
    }

    /** 설정된 모든 종목에 대해 하루치 경기를 수집한다. 저장/갱신된 경기 수를 반환. */
    public int syncDate(LocalDate date) {
        int total = 0;
        for (String category : properties.categories()) {
            try {
                total += syncCategory(category, date);
            } catch (Exception e) {
                log.warn("[sync] {} {} 수집 실패: {}", category, date, e.getMessage());
            }
        }
        log.info("[sync] {} 완료 - {}건 반영", date, total);
        return total;
    }

    private int syncCategory(String category, LocalDate date) {
        List<NaverGame> naverGames = naverSportsClient.fetchGames(category, date);
        for (NaverGame naverGame : naverGames) {
            Game game = gameRepository.findById(naverGame.gameId())
                    .map(existing -> {
                        existing.applyFrom(naverGame);
                        return existing;
                    })
                    .orElseGet(() -> Game.from(naverGame));
            gameRepository.save(game);
        }
        log.debug("[sync] {} {} - {}건", category, date, naverGames.size());
        return naverGames.size();
    }
}
