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
  const [category, setCategory] = useState<string | null>(null) // null = 전체
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

  // 카테고리(리그)별 경기 수 — 칩으로 보여준다. 등장 순서 유지.
  const categories = useMemo(() => {
    const counts = new Map<string, number>()
    for (const game of games) {
      const key = game.categoryName || '기타'
      counts.set(key, (counts.get(key) ?? 0) + 1)
    }
    return [...counts.entries()].map(([name, count]) => ({ name, count }))
  }, [games])

  // 선택한 카테고리가 오늘 목록에 없으면 자동으로 '전체'로 간주.
  const activeCategory = category && categories.some((c) => c.name === category) ? category : null

  // 같은 리그끼리 묶은 섹션. 선택된 카테고리만 보여주고,
  // 진행 중(LIVE) 경기를 섹션 안에서도, 리그 순서에서도 위로 끌어올린다.
  const sections = useMemo(() => {
    const isLive = (g: Game) => g.statusCode === 'STARTED'
    const grouped = new Map<string, Game[]>()
    for (const game of games) {
      const key = game.categoryName || '기타'
      grouped.set(key, [...(grouped.get(key) ?? []), game])
    }
    return [...grouped.entries()]
      .filter(([name]) => !activeCategory || name === activeCategory)
      .map(([name, list]) => ({
        name,
        // 진행 중 경기를 맨 위로 (나머지는 기존 시간순 유지 — sort는 안정 정렬)
        list: [...list].sort((a, b) => Number(isLive(b)) - Number(isLive(a))),
        liveCount: list.filter(isLive).length,
      }))
      // 진행 중 경기가 있는 리그를 먼저 (그 외엔 기존 순서 유지)
      .sort((a, b) => (b.liveCount > 0 ? 1 : 0) - (a.liveCount > 0 ? 1 : 0))
  }, [games, activeCategory])

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

      {categories.length > 0 && (
        <div className="category-tabs">
          <button
            className={`cat-chip ${activeCategory === null ? 'active' : ''}`}
            onClick={() => setCategory(null)}
          >
            전체 <span className="chip-count">{games.length}</span>
          </button>
          {categories.map(({ name, count }) => (
            <button
              key={name}
              className={`cat-chip ${activeCategory === name ? 'active' : ''}`}
              onClick={() => setCategory(name)}
            >
              {name} <span className="chip-count">{count}</span>
            </button>
          ))}
        </div>
      )}

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

      {sections.map(({ name, list, liveCount }) => (
        <section key={name} className="category">
          <h2 className="category-title">
            {name} <span className="count">{list.length}</span>
            {liveCount > 0 && <span className="live-tag"><span className="live-dot" />LIVE</span>}
          </h2>
          <div className="games">
            {list.map((game) => (
              <GameCard key={game.gameId} game={game} />
            ))}
          </div>
        </section>
      ))}

      <footer className="footer">데이터 출처: 네이버 스포츠 · 1분마다 자동 갱신</footer>
    </div>
  )
}
