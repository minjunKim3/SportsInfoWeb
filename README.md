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

## 경기 볼 가치 분석 (AI)

경기를 누르면 **"이 경기가 볼 가치가 있는지(상위 %), 전세계 예상 시청자 수, 어느 팀이 더 간절한지"** 를 분석해줍니다.
정확성이 생명이라, **웹검색 기반**으로 실제 최신 정보(순위·기록·스타 선수 출전 등)를 수집한 뒤 판단합니다.

- 엔진: **Google Gemini 무료 등급 + Google Search grounding** (웹검색 내장)
- 결과는 경기당 캐시해 무료 quota를 절약하며, "새로 분석"으로 갱신 가능
- 결과는 AI 추정치이므로 응원팀 고르기 등 재미 용도로 사용

> 무료지만 키가 필요합니다. [Google AI Studio](https://aistudio.google.com/apikey)에서 무료 API 키를 발급받아
> 환경변수 `GEMINI_API_KEY`에 넣고 백엔드를 실행하세요. 키가 없으면 분석 버튼이 안내 메시지를 보여줍니다.

## 기술 스택

- **Backend**: Java 21, Spring Boot 4, Spring Data JPA, H2 (→ PostgreSQL 예정)
- **Frontend**: React 19, TypeScript, Vite
- **AI 분석**: Google Gemini (무료 등급, 웹검색 grounding)

## 실행 방법

```bash
# 백엔드 (분석 기능을 쓰려면 GEMINI_API_KEY 설정)
cd backend
GEMINI_API_KEY=발급받은_키 ./gradlew bootRun   # Windows PowerShell: $env:GEMINI_API_KEY="키"; ./gradlew bootRun

# 프론트엔드
cd frontend
npm install && npm run dev
```

- 화면: `http://localhost:5173`
- API: `http://localhost:8080/api/games?broadcastOnly=true`
- 경기 분석: `POST http://localhost:8080/api/games/{gameId}/analysis`
- 수동 수집: `POST http://localhost:8080/api/sync`
- DB 콘솔: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/sportsinfo`)

## 데이터 출처에 대해

경기 데이터는 네이버 스포츠의 비공식 API에서 수집합니다.
학습 목적의 개인 프로젝트이며, 수집 주기를 보수적으로 설정해(하루 일정은 일 1회, 실시간 상태는 5분 간격) 대상 서버에 부담을 주지 않도록 했습니다.
