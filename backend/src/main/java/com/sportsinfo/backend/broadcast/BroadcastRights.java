package com.sportsinfo.backend.broadcast;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 broadcast.rights.* 를 바인딩한 리그(categoryId)별 중계 채널 표.
 * 표가 깔끔하므로 조회는 LLM 없이 결정적으로 한다(환각 방지).
 * 중계권은 시즌마다 바뀌므로 코드가 아닌 설정으로 빼서 갱신을 쉽게 했다.
 */
@ConfigurationProperties(prefix = "broadcast")
public record BroadcastRights(Map<String, Entry> rights) {

    public record Entry(List<String> channels, String note) {
    }

    /** categoryId로 중계권을 찾는다. 없으면 null. */
    public Entry lookup(String categoryId) {
        if (categoryId == null || rights == null) {
            return null;
        }
        return rights.get(categoryId);
    }
}
