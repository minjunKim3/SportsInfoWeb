package com.sportsinfo.backend.broadcast;

import java.util.List;

/**
 * 한 경기의 "한국어 중계" 해석 결과.
 * 사용자가 원한 두 가지 질문에 답한다: (1) 중계가 있는가? (2) 어디서 보는가?
 */
public record BroadcastInfo(
        boolean available,
        List<String> channels,
        BroadcastSource source,
        String note
) {

    public static BroadcastInfo unknown() {
        return new BroadcastInfo(false, List.of(), BroadcastSource.UNKNOWN, null);
    }
}
