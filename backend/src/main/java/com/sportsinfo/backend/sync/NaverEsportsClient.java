package com.sportsinfo.backend.sync;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsinfo.backend.sync.NaverScheduleResponse.NaverGame;

/**
 * 네이버 e스포츠(LCK 등)는 일반 스포츠와 다른 시스템을 쓴다.
 * 일정 API 파라미터가 비공개라, 일정 페이지가 서버에서 미리 심어 보내는
 * Next.js의 __NEXT_DATA__(JSON)를 추출해 경기를 읽어온다.
 * 결과를 공통 모델(NaverGame)로 변환해 기존 수집 파이프라인에 그대로 태운다.
 */
@Component
public class NaverEsportsClient {

    private static final Logger log = LoggerFactory.getLogger(NaverEsportsClient.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // <script id="__NEXT_DATA__" ...>{...}</script> 안의 JSON 본문만 캡처.
    private static final Pattern NEXT_DATA = Pattern.compile(
            "<script id=\"__NEXT_DATA__\"[^>]*>(.*?)</script>", Pattern.DOTALL);

    // topLeagueId -> 화면에 보일 리그명. 없으면 대문자 ID로 폴백.
    private static final Map<String, String> LEAGUE_NAMES = Map.of(
            "lck", "LCK",
            "worlds", "롤드컵",
            "msi", "MSI");

    // __NEXT_DATA__ JSON 트리 파싱 전용. Boot가 ObjectMapper 빈을 만들지 않으므로 직접 생성한다.
    private final ObjectMapper objectMapper = new ObjectMapper();
    // 리다이렉트를 따라가도록 명시(RestClient 기본값은 본문을 못 받아오는 문제가 있었음).
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String scheduleUrl;

    public NaverEsportsClient(NaverSportsProperties properties) {
        this.scheduleUrl = properties.esportsScheduleUrl();
    }

    /** e스포츠 일정 페이지가 담고 있는 경기 전체(보통 오늘 전후 윈도우). */
    public List<NaverGame> fetchGames() {
        String html;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(scheduleUrl))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            html = response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (Exception e) {
            log.warn("[esports] 일정 페이지 요청 실패: {}", e.getMessage());
            return List.of();
        }
        if (html == null || html.isBlank()) {
            return List.of();
        }

        Matcher matcher = NEXT_DATA.matcher(html);
        if (!matcher.find()) {
            log.warn("[esports] __NEXT_DATA__ 를 찾지 못함 - 페이지 구조 변경 가능성");
            return List.of();
        }

        List<NaverGame> games = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(matcher.group(1));
            collectMatches(root, games);
        } catch (Exception e) {
            log.warn("[esports] __NEXT_DATA__ 파싱 실패: {}", e.getMessage());
            return List.of();
        }
        log.debug("[esports] {}경기 추출", games.size());
        return games;
    }

    /** JSON 트리를 훑어 'homeTeam'을 가진 노드(=경기)를 찾아 NaverGame으로 변환한다. */
    private void collectMatches(JsonNode node, List<NaverGame> out) {
        if (node.isObject()) {
            if (node.has("homeTeam") && node.has("gameId")) {
                out.add(toGame(node));
                return;
            }
            node.forEach(child -> collectMatches(child, out));
        } else if (node.isArray()) {
            node.forEach(child -> collectMatches(child, out));
        }
    }

    private NaverGame toGame(JsonNode m) {
        JsonNode home = m.path("homeTeam");
        JsonNode away = m.path("awayTeam");
        LocalDateTime startAt = Instant.ofEpochMilli(m.path("startDate").asLong())
                .atZone(KST).toLocalDateTime();
        String topLeagueId = text(m, "topLeagueId");
        String categoryName = LEAGUE_NAMES.getOrDefault(
                topLeagueId, topLeagueId == null ? "e스포츠" : topLeagueId.toUpperCase());

        return new NaverGame(
                text(m, "gameId"),
                "esports",
                topLeagueId,
                categoryName,
                startAt.toLocalDate(),
                startAt,
                text(m, "stadium"),
                text(m, "title"),
                text(home, "nameAcronym"),
                text(home, "name"),
                intOrNull(m, "homeScore"),
                text(home, "imageUrl"),
                text(away, "nameAcronym"),
                text(away, "name"),
                intOrNull(m, "awayScore"),
                text(away, "imageUrl"),
                text(m, "matchStatus"),   // RESULT / STARTED / BEFORE - 스포츠와 동일
                null,
                false,
                false,
                null                      // 중계 채널은 BroadcastResolver가 lck 중계권 표로 채움
        );
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }

    private static Integer intOrNull(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asInt();
    }
}
