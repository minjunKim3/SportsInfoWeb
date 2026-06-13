package com.sportsinfo.backend.analysis;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GameAnalysisController {

    private final GameAnalysisService analysisService;

    public GameAnalysisController(GameAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * 경기 1건의 볼 가치 분석. 웹검색 기반이라 시간이 걸릴 수 있다.
     * refresh=true면 캐시를 무시하고 새로 분석한다.
     */
    @PostMapping("/{gameId}/analysis")
    public GameAnalysisDto analyze(@PathVariable String gameId,
                                   @RequestParam(defaultValue = "false") boolean refresh) {
        return analysisService.analyze(gameId, refresh);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleNotConfigured(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
    }
}
