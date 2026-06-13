package com.sportsinfo.backend.broadcast;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sportsinfo.backend.game.Game;

/**
 * 경기 하나의 중계 정보를 해석한다. 신뢰도 높은 출처부터 차례로 시도하는 하이브리드:
 *   1) 네이버가 준 broadChannel        → 가장 신뢰
 *   2) 리그→채널 중계권 표(코드 매핑)   → 네이버가 비워둔 경기를 결정적으로 채움
 *   3) 그래도 모르면 UNKNOWN            → 2단계에서 LLM이 편성표 파싱으로 채울 자리
 *
 * 리그→채널은 표가 깔끔해 코드로 처리한다. LLM은 표로 못 만드는 비정형 작업에만 쓴다.
 */
@Component
public class BroadcastResolver {

    private final BroadcastRights broadcastRights;

    public BroadcastResolver(BroadcastRights broadcastRights) {
        this.broadcastRights = broadcastRights;
    }

    public BroadcastInfo resolve(Game game) {
        // 1) 네이버 직접 제공
        if (game.hasBroadcast()) {
            return new BroadcastInfo(true, List.of(game.getBroadChannel()), BroadcastSource.NAVER, null);
        }

        // 2) 중계권 표로 채우기
        var entry = broadcastRights.lookup(game.getCategoryId());
        if (entry != null && entry.channels() != null && !entry.channels().isEmpty()) {
            return new BroadcastInfo(true, entry.channels(), BroadcastSource.KNOWN_RIGHTS, entry.note());
        }

        // 3) 아직 모름
        return BroadcastInfo.unknown();
    }
}
