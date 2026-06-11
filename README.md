# SportsInfoWeb

하루 동안 열리는 **한국어 중계가 있는 스포츠 경기**를 한곳에 모아 보여주는 웹 서비스입니다.
야구(KBO/MLB), 축구(K리그/해외축구), 농구, 배구, e스포츠의 일정·실시간 스코어·중계 채널을 제공합니다.

> 취업 준비용 사이드 프로젝트로, 설계 의도와 트러블슈팅 과정을 함께 기록합니다.

## 아키텍처

```
[네이버 스포츠 비공식 API]
        │  (스케줄러가 주기적으로 호출)
        ▼
┌─────────────────────────────┐
│  Spring Boot Backend        │
│                             │
│  GameSyncScheduler          │  오늘 경기: 5분 간격
│    └─ GameSyncService       │  주간 일정: 매일 새벽 5시
│         └─ NaverSportsClient│  외부 API 호출 격리
│         └─ GameRepository   │  gameId 기준 upsert
│                             │
│  GameController             │  GET /api/games?date=&broadcastOnly=
└─────────────────────────────┘
        │
        ▼
[React Frontend (예정)]
```

**왜 수집해서 저장하나?** 비공식 API에 대한 의존을 격리하기 위해서입니다.
네이버 쪽 스펙 변경·장애가 발생해도 마지막 수집본으로 서비스가 유지되고,
프론트엔드 트래픽이 외부 API로 그대로 전달되지 않아 요청량을 통제할 수 있습니다.

## 기술 스택

- **Backend**: Java 21, Spring Boot 4, Spring Data JPA, H2 (→ PostgreSQL 예정)
- **Frontend**: React (예정)

## 실행 방법

```bash
cd backend
./gradlew bootRun
```

- API: `http://localhost:8080/api/games?broadcastOnly=true`
- 수동 수집: `POST http://localhost:8080/api/sync`
- DB 콘솔: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/sportsinfo`)

## 데이터 출처에 대해

경기 데이터는 네이버 스포츠의 비공식 API에서 수집합니다.
학습 목적의 개인 프로젝트이며, 수집 주기를 보수적으로 설정해(하루 일정은 일 1회, 실시간 상태는 5분 간격) 대상 서버에 부담을 주지 않도록 했습니다.
