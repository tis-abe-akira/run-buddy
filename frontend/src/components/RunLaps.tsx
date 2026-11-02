import type { RunDetail } from '../api/client'
import { formatDuration, formatPace } from '../lib/format'

export type RunLapsState =
  | { status: 'loading' }
  | { status: 'error'; message: string; onRetry: () => void }
  | { status: 'ready'; detail: RunDetail }

/**
 * A run's laps, read from its activity file (ADR-0012). The first look downloads the file, so this
 * part loads on its own while the rest of the run is already shown.
 */
export function RunLaps({ state }: { state: RunLapsState }) {
  return (
    <section className="stack run-laps" aria-label="ラップ" style={{ gap: 'var(--spacing-sm)' }}>
      <h3 className="caption-uppercase muted">ラップ</h3>
      <Body state={state} />
    </section>
  )
}

function Body({ state }: { state: RunLapsState }) {
  switch (state.status) {
    case 'loading':
      return (
        <p className="body-sm muted" role="status" style={{ margin: 0 }}>
          ラップを読み込み中…
        </p>
      )
    case 'error':
      return (
        <div className="row">
          <span className="field-error" role="alert">
            {state.message}
          </span>
          <button type="button" className="button button-secondary" onClick={state.onRetry}>
            再試行
          </button>
        </div>
      )
    case 'ready':
      if (!state.detail.available) {
        return (
          <p className="body-sm muted" style={{ margin: 0 }}>
            このランには詳細データがありません
          </p>
        )
      }
      if (state.detail.laps.length === 0) {
        return (
          <p className="body-sm muted" style={{ margin: 0 }}>
            ラップの記録はありません
          </p>
        )
      }
      return (
        <div className="table-scroll">
          <table className="table">
            <thead>
              <tr>
                <th className="num">#</th>
                <th className="num">km</th>
                <th className="num">タイム</th>
                <th className="num">ペース</th>
                <th className="num">心拍</th>
                <th className="num">ケイデンス</th>
                <th className="num">上昇 m</th>
              </tr>
            </thead>
            <tbody>
              {state.detail.laps.map((lap) => (
                <tr key={lap.number}>
                  <td className="num">{lap.number}</td>
                  <td className="num">{lap.distanceKm.toFixed(2)}</td>
                  <td className="num">{formatDuration(lap.durationSeconds)}</td>
                  <td className="num">{formatPace(lap.paceSecondsPerKm)}</td>
                  <td className="num">
                    {lap.averageHeartRate === null ? '—' : `${lap.averageHeartRate} / ${lap.maxHeartRate ?? '—'}`}
                  </td>
                  <td className="num">{lap.averageCadence ?? '—'}</td>
                  <td className="num">{lap.elevationGainMeters ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )
  }
}
