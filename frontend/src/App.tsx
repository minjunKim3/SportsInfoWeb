import { useCallback, useEffect, useMemo, useState } from 'react'
import { fetchGames, type Game } from './api'
import { GameCard } from './GameCard'

function toDateString(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatLabel(dateString: string): string {
  const [year, month, day] = dateString.split('-').map(Number)
  const weekday = ['일', '월', '화', '수', '목', '금', '토'][new Date(year, month - 1, day).getDay()]
  return `${month}월 ${day}일 (${weekday})`
}

const REFRESH_INTERVAL_MS = 60_000

export default function App() {
  const today = toDateString(new Date())
  const [date, setDate] = useState(today)
  const [broadcastOnly, setBroadcastOnly] = useState(false)
  const [games, setGames] = useState<Game[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    try {
      setError(null)
      setGames(await fetchGames(date, broadcastOnly))
    } catch (e) {
      setError(e instanceof Error ? e.message : '알 수 없는 오류')
    } finally {
      setLoading(false)
    }
  }, [date, broadcastOnly])

  useEffect(() => {
    setLoading(true)
    load()
    // 오늘 화면을 보고 있을 때만 1분마다 자동 갱신 (진행 중 스코어 반영)
    if (date !== today) return
    const timer = setInterval(load, REFRESH_INTERVAL_MS)
    return () => clearInterval(timer)
  }, [load, date, today])

  const moveDate = (offset: number) => {
    const [year, month, day] = date.split('-').map(Number)
    setDate(toDateString(new Date(year, month - 1, day + offset)))
  }

  // 같은 리그(categoryName)끼리 묶어서 섹션으로 보여준다.
  const sections = useMemo(() => {
    const grouped = new Map<string, Game[]>()
    for (const game of games) {
      const key = game.categoryName || '기타'
      grouped.set(key, [...(grouped.get(key) ?? []), game])
    }
    return [...grouped.entries()]
  }, [games])

  const liveCount = games.filter((g) => g.statusCode === 'STARTED').length

  return (
    <div className="app">
      <header className="header">
        <h1 className="logo">
          Sports<span>Info</span>
        </h1>
        <p className="tagline">한국어 중계가 있는 오늘의 경기를 한곳에서</p>
      </header>

      <div className="toolbar">
        <div className="date-nav">
          <button onClick={() => moveDate(-1)} aria-label="이전 날짜">‹</button>
          <div className="date-label">
            {formatLabel(date)}
            {date === today && <span className="today-badge">오늘</span>}
          </div>
          <button onClick={() => moveDate(1)} aria-label="다음 날짜">›</button>
        </div>
        <label className="filter">
          <input
            type="checkbox"
            checked={broadcastOnly}
            onChange={(e) => setBroadcastOnly(e.target.checked)}
          />
          중계 있는 경기만
        </label>
      </div>

      {liveCount > 0 && (
        <div className="live-banner">
          <span className="live-dot" /> 지금 {liveCount}경기 진행 중
        </div>
      )}

      {error && <div className="message error">{error}</div>}
      {loading && !error && <div className="message">불러오는 중...</div>}
      {!loading && !error && games.length === 0 && (
        <div className="message">이 날짜에 수집된 경기가 없어요.</div>
      )}

      {sections.map(([categoryName, sectionGames]) => (
        <section key={categoryName} className="category">
          <h2 className="category-title">
            {categoryName} <span className="count">{sectionGames.length}</span>
          </h2>
          <div className="games">
            {sectionGames.map((game) => (
              <GameCard key={game.gameId} game={game} />
            ))}
          </div>
        </section>
      ))}

      <footer className="footer">데이터 출처: 네이버 스포츠 · 1분마다 자동 갱신</footer>
    </div>
  )
}
