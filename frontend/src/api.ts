// 백엔드(GameDto)와 1:1로 맞춘 타입. 백엔드 응답이 바뀌면 여기만 고치면 된다.
export interface Game {
  gameId: string
  superCategoryId: string
  categoryId: string
  categoryName: string
  gameDateTime: string
  stadium: string | null
  title: string | null
  homeTeamName: string | null
  homeTeamScore: number | null
  homeTeamEmblemUrl: string | null
  awayTeamName: string | null
  awayTeamScore: number | null
  awayTeamEmblemUrl: string | null
  statusCode: 'BEFORE' | 'STARTED' | 'RESULT' | string
  statusInfo: string | null
  cancelled: boolean
  broadcast: BroadcastInfo
}

// 중계 정보. source로 신뢰도를 구분한다.
//  - NAVER: 네이버 편성 (확정)
//  - KNOWN_RIGHTS: 리그별 중계권 표로 추정
//  - UNKNOWN: 아직 모름
export interface BroadcastInfo {
  available: boolean
  channels: string[]
  source: 'NAVER' | 'KNOWN_RIGHTS' | 'UNKNOWN'
  note: string | null
}

export async function fetchGames(date: string, broadcastOnly: boolean): Promise<Game[]> {
  const params = new URLSearchParams({ date, broadcastOnly: String(broadcastOnly) })
  const response = await fetch(`/api/games?${params}`)
  if (!response.ok) {
    throw new Error(`경기 목록을 불러오지 못했어요 (HTTP ${response.status})`)
  }
  return response.json()
}

export interface AnalysisSource {
  title: string | null
  uri: string
}

export interface GameAnalysis {
  gameId: string
  analysisMarkdown: string
  sources: AnalysisSource[]
  createdAt: string
  cached: boolean
}

// 경기 볼 가치 분석. 웹검색 기반이라 수십 초 걸릴 수 있다.
export async function fetchAnalysis(gameId: string, refresh = false): Promise<GameAnalysis> {
  const params = new URLSearchParams({ refresh: String(refresh) })
  const response = await fetch(`/api/games/${gameId}/analysis?${params}`, { method: 'POST' })
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(body?.error ?? `분석에 실패했어요 (HTTP ${response.status})`)
  }
  return response.json()
}
