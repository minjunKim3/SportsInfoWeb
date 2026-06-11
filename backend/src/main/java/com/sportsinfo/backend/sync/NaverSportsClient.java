package com.sportsinfo.backend.sync;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.sportsinfo.backend.sync.NaverScheduleResponse.NaverGame;

/**
 * 네이버 스포츠 비공식 API 호출 담당. 외부 HTTP 통신은 전부 이 클래스 뒤로 숨겨서,
 * 나중에 API 스펙이 바뀌어도 여기만 고치면 되도록 한다.
 */
@Component
public class NaverSportsClient {

    private static final String FIELDS = String.join(",",
            "basic", "superCategoryId", "categoryName", "stadium",
            "statusInfo", "gameOnAir", "hasVideo", "title", "broadChannel");

    private final RestClient restClient;

    public NaverSportsClient(NaverSportsProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build();
    }

    /** 특정 종목(upperCategoryId)의 하루치 경기 목록을 가져온다. */
    public List<NaverGame> fetchGames(String upperCategoryId, LocalDate date) {
        NaverScheduleResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/schedule/games")
                        .queryParam("fields", FIELDS)
                        .queryParam("upperCategoryId", upperCategoryId)
                        .queryParam("fromDate", date.toString())
                        .queryParam("toDate", date.toString())
                        .queryParam("size", 500)
                        .build())
                .retrieve()
                .body(NaverScheduleResponse.class);

        if (response == null || !response.success() || response.result() == null
                || response.result().games() == null) {
            return List.of();
        }
        return response.result().games();
    }
}
