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

  const run = async (refresh: boolean) => {
    setLoading(true)
    setError(null)
    try {
      setResult(await fetchAnalysis(gameId, refresh))
    } catch (e) {
      setError(e instanceof Error ? e.message : '알 수 없는 오류')
    } finally {
      setLoading(false)
    }
  }

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
          <div
            className="analysis-body"
            dangerouslySetInnerHTML={{ __html: renderMarkdown(result.analysisMarkdown) }}
          />

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
