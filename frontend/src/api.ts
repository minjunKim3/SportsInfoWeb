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
  broadChannel: string | null
}

export async function fetchGames(date: string, broadcastOnly: boolean): Promise<Game[]> {
  const params = new URLSearchParams({ date, broadcastOnly: String(broadcastOnly) })
  const response = await fetch(`/api/games?${params}`)
  if (!response.ok) {
    throw new Error(`경기 목록을 불러오지 못했어요 (HTTP ${response.status})`)
  }
  return response.json()
}
