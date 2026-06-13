import type { Game } from './api'
import { AnalysisPanel } from './AnalysisPanel'

function formatTime(dateTime: string): string {
  const time = dateTime.split('T')[1] ?? ''
  return time.slice(0, 5)
}

function StatusBadge({ game }: { game: Game }) {
  if (game.cancelled) return <span className="badge cancelled">취소</span>
  switch (game.statusCode) {
    case 'STARTED':
      return (
        <span className="badge live">
          <span className="live-dot" />
          {game.statusInfo || 'LIVE'}
        </span>
      )
    case 'RESULT':
      return <span className="badge result">종료</span>
    default:
      return <span className="badge before">{formatTime(game.gameDateTime)} 예정</span>
  }
}

function TeamRow({ name, score, emblemUrl, isWinning }: {
  name: string
  score: number | null
  emblemUrl: string | null
  isWinning: boolean
}) {
  return (
    <div className={`team ${isWinning ? 'winning' : ''}`}>
      {emblemUrl
        ? <img className="emblem" src={emblemUrl} alt="" loading="lazy" referrerPolicy="no-referrer" />
        : <span className="emblem placeholder" />}
      <span className="team-name">{name}</span>
      <span className="score">{score ?? '-'}</span>
    </div>
  )
}

export function GameCard({ game }: { game: Game }) {
  // 팀 대결이 아닌 콘텐츠(편파중계, 특집 방송 등)는 제목만 보여준다.
  const isTeamMatch = Boolean(game.homeTeamName && game.awayTeamName)
  const started = game.statusCode === 'STARTED' || game.statusCode === 'RESULT'
  const homeScore = game.homeTeamScore ?? 0
  const awayScore = game.awayTeamScore ?? 0

  return (
    <article className={`card ${game.statusCode === 'STARTED' ? 'is-live' : ''}`}>
      <div className="card-top">
        <StatusBadge game={game} />
        {game.stadium && <span className="stadium">{game.stadium}</span>}
      </div>

      {isTeamMatch ? (
        <div className="teams">
          <TeamRow
            name={game.awayTeamName!}
            score={started ? awayScore : null}
            emblemUrl={game.awayTeamEmblemUrl}
            isWinning={started && awayScore > homeScore}
          />
          <TeamRow
            name={game.homeTeamName!}
            score={started ? homeScore : null}
            emblemUrl={game.homeTeamEmblemUrl}
            isWinning={started && homeScore > awayScore}
          />
        </div>
      ) : (
        <p className="content-title">{game.title || '경기 정보 없음'}</p>
      )}

      {game.broadcast.available && (
        <div className="channel">
          📺 {game.broadcast.channels.join(', ')}
          {game.broadcast.source === 'KNOWN_RIGHTS' && (
            <span className="channel-badge" title="리그별 중계권 정보로 추정한 채널이에요">추정</span>
          )}
        </div>
      )}

      {isTeamMatch && <AnalysisPanel gameId={game.gameId} />}
    </article>
  )
}
