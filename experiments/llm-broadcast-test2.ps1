# LLM 중계 매칭 개념 검증 v2
# v1의 교훈: (1) 배치로 주면 답이 섞이고 유령 항목 생성 (2) 선입견이 근거를 이김.
# 처방: 경기를 하나씩 처리 + "추론"이 아닌 "기계적 대조" 작업으로 재정의 + thinking 끄기.
param([string]$Model = "qwen3:8b")

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# 근거 데이터: 리그명 -> 한국 중계 채널. 실서비스에선 공식 편성표 크롤링으로 대체.
$kb = @"
- 프리미어리그(EPL): 쿠팡플레이
- 분데스리가: 쿠팡플레이
- MLB(메이저리그): SPOTV, 쿠팡플레이
- KBO리그: KBS N SPORTS, MBC SPORTS+, SBS SPORTS, SPOTV
- NBA: SPOTV, 쿠팡플레이
- LCK: 네이버, 치지직, 아프리카TV
"@

# (리그명, 기대 정답) 테스트 케이스
$cases = @(
    @{ league = "프리미어리그"; expect = "쿠팡플레이" },
    @{ league = "메이저리그";   expect = "SPOTV/쿠팡플레이" },
    @{ league = "국가대표 친선전"; expect = "false (KB에 없음)" },
    @{ league = "KLPGA";        expect = "false (KB에 없음)" }
)

Write-Host "=== 모델: $Model (경기당 1회 호출) ===" -ForegroundColor Cyan

foreach ($c in $cases) {
    # /no_think: Qwen3의 추론 토큰을 꺼서 출력을 깔끔하게.
    $prompt = @"
/no_think
아래 [중계권 표]에서 입력 리그와 '같은 대회'를 가리키는 줄을 찾아라.
- 찾으면: 그 줄에 적힌 채널만 그대로 출력.
- 표에 없으면: available=false. 절대 다른 리그의 채널을 가져오지 말 것.
- 표에 없는 채널을 새로 만들지 말 것.

[중계권 표]
$kb

[입력 리그]
"$($c.league)"

JSON만 출력:
{"koreanBroadcastAvailable": true/false, "channels": [], "reason": "어느 표의 줄과 매칭했는지"}
"@

    $body = @{
        model = $Model
        prompt = $prompt
        stream = $false
        format = "json"
        options = @{ temperature = 0 }
    } | ConvertTo-Json -Depth 5

    $r = Invoke-RestMethod -Uri "http://localhost:11434/api/generate" -Method Post -Body $body -ContentType "application/json" -TimeoutSec 180
    Write-Host "`n[$($c.league)] 기대: $($c.expect)" -ForegroundColor Yellow
    Write-Host $r.response.Trim()
}
