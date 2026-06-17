import { useState } from 'react'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { fetchAnalysis, type GameAnalysis } from './api'

// 마크다운 → 안전한 HTML. LLM 출력이므로 DOMPurify로 XSS를 차단한다.
function renderMarkdown(md: string): string {
  return DOMPurify.sanitize(marked.parse(md, { async: false }) as string)
}

export function AnalysisPanel({ gameId }: { gameId: string }) {
  const [result, setResult] = useState<GameAnalysis | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [open, setOpen] = useState<Record<number, boolean>>({})

  const run = async (refresh: boolean) => {
    setLoading(true)
    setError(null)
    setOpen({})
    try {
      setResult(await fetchAnalysis(gameId, refresh))
    } catch (e) {
      setError(e instanceof Error ? e.message : '알 수 없는 오류')
    } finally {
      setLoading(false)
    }
  }

  const toggle = (i: number) => setOpen((prev) => ({ ...prev, [i]: !prev[i] }))

  if (!result && !loading && !error) {
    return (
      <button className="analyze-btn" onClick={() => run(false)}>
        🔍 볼 가치 분석
      </button>
    )
  }

  return (
    <div className="analysis">
      {loading && (
        <div className="analysis-loading">
          <span className="live-dot" /> 웹검색하며 분석 중... (최대 1분)
        </div>
      )}

      {error && (
        <div className="analysis-error">
          {error}
          <button className="analyze-btn small" onClick={() => run(false)}>다시 시도</button>
        </div>
      )}

      {result && !loading && (
        <>
          {/* 요약: 한국어 중계처 + 충족 여부 + 예상 시청자 (항상 보임) */}
          <div className="analysis-summary">
            {result.koreanBroadcast && (
              <div className="broadcast-where">📺 한국어 중계: <strong>{result.koreanBroadcast}</strong></div>
            )}
            <div className={`verdict ${result.meetsThreshold ? 'yes' : 'no'}`}>
              <span className="verdict-mark">{result.meetsThreshold ? '✅ 볼 만한 경기' : '➖ 상위권은 아님'}</span>
              {result.verdict && <span className="verdict-line">{result.verdict}</span>}
            </div>
            {result.expectedViewers && (
              <div className="viewers">👀 {result.expectedViewers}</div>
            )}
          </div>

          {/* 상세: 항목별 아코디언 (클릭해서 펼침) */}
          <div className="accordion">
            {result.sections.map((s, i) => (
              <div className={`acc-item ${open[i] ? 'open' : ''}`} key={i}>
                <button className="acc-header" onClick={() => toggle(i)}>
                  <span>{s.title}</span>
                  <span className="acc-arrow">{open[i] ? '▾' : '▸'}</span>
                </button>
                {open[i] && (
                  <div
                    className="acc-body"
                    dangerouslySetInnerHTML={{ __html: renderMarkdown(s.content) }}
                  />
                )}
              </div>
            ))}
          </div>

          {result.sources.length > 0 && (
            <div className="analysis-sources">
              <span className="sources-label">출처</span>
              {result.sources.map((s, i) => (
                <a key={i} href={s.uri} target="_blank" rel="noreferrer noopener">
                  {s.title || new URL(s.uri).hostname}
                </a>
              ))}
            </div>
          )}

          <div className="analysis-foot">
            <span className="disclaimer">※ AI 추정치예요 (응원팀 고르기용)</span>
            <button className="analyze-btn small" onClick={() => run(true)}>새로 분석</button>
          </div>
        </>
      )}
    </div>
  )
}
