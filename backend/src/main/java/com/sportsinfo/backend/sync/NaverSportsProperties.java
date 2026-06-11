package com.sportsinfo.backend.sync;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 naver.sports.* 설정을 묶어서 주입받는다.
 * 수집할 종목 목록을 코드가 아닌 설정으로 빼서, 종목 추가/제거 시 코드 수정이 없도록 한다.
 */
@ConfigurationProperties(prefix = "naver.sports")
public record NaverSportsProperties(
        String baseUrl,
        List<String> categories
) {
}
