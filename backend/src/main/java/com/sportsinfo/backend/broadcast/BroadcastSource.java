package com.sportsinfo.backend.broadcast;

/** 중계 정보를 어디서 알아냈는지. 신뢰도 순서이자, 프론트에서 배지로 구분하는 근거. */
public enum BroadcastSource {
    /** 네이버가 직접 준 편성 정보. 가장 신뢰. */
    NAVER,
    /** 리그→채널 중계권 표(broadcast-rights.json)로 코드가 채운 값. */
    KNOWN_RIGHTS,
    /** 아직 모름. 2단계에서 LLM이 편성표를 파싱해 채울 자리. */
    UNKNOWN
}
