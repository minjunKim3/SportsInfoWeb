# LLM 중계 매칭 개념 검증 스크립트
# 목적: "근거(편성권 KB) + 경기 정보 → 중계 가능 여부/채널 JSON" 이 실제로 되는지 확인.
# 모델을 인자로 받아 여러 모델을 같은 조건으로 비교할 수 있게 한다.
param([string]$Model = "gemma3:4b")

# --- 근거 데이터(RAG의 retrieval 부분): 2026 한국 스포츠 중계권 요약 ---
# 실제 서비스에서는 이 부분을 공식 편성표 크롤링으로 대체한다. 지금은 개념 검증용 고정 KB.
$knowledgeBase = @"
[2026 한국 스포츠 중계권 정보]
- 프리미어리그(EPL, 잉글랜드 프로축구): 쿠팡플레이 독점 중계
- 해외축구 국가대표 친선경기: 중계권 미정, 채널별로 상이 (확실하지 않음)
- MLB(메이저리그 야구): SPOTV, 쿠팡플레이
- KBO리그(국내야구): KBS N SPORTS, MBC SPORTS+, SBS SPORTS, SPOTV (경기별 분배)
- NBA(미국 프로농구): SPOTV, 쿠팡플레이
- LCK(롤 e스포츠): 네이버, 치지직, 아프리카TV
- 분데스리가(독일 축구): 쿠팡플레이
"@

# --- 입력 경기들 (네이버 API에서 받은 형태를 모사) ---
# 일부러 다양한 케이스를 섞었다: 이미 채널 있음 / KB로 추론 가능 / KB에 없음
$games = @"
[판정할 경기 목록]
1. categoryName="프리미어리그", 토트넘 vs 맨체스터 시티
2. categoryName="메이저리그", 다저스 vs 샌디에이고
3. categoryName="국가대표 친선전", 포르투갈 vs 나이지리아
4. categoryName="KLPGA", 여자골프 대회 (팀 경기 아님)
"@

$prompt = @"
너는 스포츠 중계 정보 분석기다. 아래 [중계권 정보]만을 근거로, 각 경기의 한국어 중계 가능 여부와 채널을 판단하라.
중요 규칙:
- 반드시 주어진 [중계권 정보]에 근거할 것. 정보에 없으면 추측하지 말고 available=false, confidence="low"로 둘 것.
- 근거가 명확하면 confidence="high", 정보에 '미정/불확실'이 있으면 "low".
- 채널은 [중계권 정보]에 적힌 것만 사용. 새로 지어내지 말 것.

$knowledgeBase

$games

다음 JSON 형식으로만 답하라(설명 금지). results 배열 안에 경기별로 한 항목씩:
{"results": [{"index": 1, "koreanBroadcastAvailable": true, "channels": ["쿠팡플레이"], "confidence": "high", "reason": "근거 한 줄"}]}
"@

$body = @{
    model = $Model
    prompt = $prompt
    stream = $false
    format = "json"
    options = @{ temperature = 0 }
} | ConvertTo-Json -Depth 5

Write-Host "=== 모델: $Model ===" -ForegroundColor Cyan
$sw = [System.Diagnostics.Stopwatch]::StartNew()
$response = Invoke-RestMethod -Uri "http://localhost:11434/api/generate" -Method Post -Body $body -ContentType "application/json" -TimeoutSec 180
$sw.Stop()
Write-Host "소요: $([math]::Round($sw.Elapsed.TotalSeconds,1))초`n" -ForegroundColor DarkGray
$response.response
