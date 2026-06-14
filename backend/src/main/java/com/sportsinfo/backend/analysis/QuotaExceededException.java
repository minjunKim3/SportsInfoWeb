package com.sportsinfo.backend.analysis;

/** Gemini 무료 quota 초과(429)를 사용자 친화적으로 전달하기 위한 예외. */
public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(String message) {
        super(message);
    }
}
