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
type Props = {
  state: RunLapsState
  /** The lap highlighted on the map. */
  selectedLap?: number | null
  /** Picks a lap to highlight on the map (null lets go). Absent when there is no map, so nothing is pickable. */
  onSelectLap?: (lap: number | null) => void
}

export function RunLaps({ state, selectedLap = null, onSelectLap }: Props) {
  return (
    <section className="stack run-laps" aria-label="ラップ" style={{ gap: 'var(--spacing-sm)' }}>
      <h3 className="caption-uppercase muted">ラップ</h3>
      <Body state={state} selectedLap={selectedLap} onSelectLap={onSelectLap} />
    </section>
  )
}

function Body({ state, selectedLap, onSelectLap }: Props) {
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
        <div className="table-scroll lap-table-scroll">
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
              {state.detail.laps.map((lap) => {
                const selected = lap.number === selectedLap
                const toggle = () => onSelectLap?.(selected ? null : lap.number)
                return (
                  <tr
                    key={lap.number}
                    className={onSelectLap ? (selected ? 'lap-row is-selected' : 'lap-row') : undefined}
                    onClick={onSelectLap ? toggle : undefined}
                  >
                    <td className="num">
                      {onSelectLap ? (
                        <button
                          type="button"
                          className="lap-select"
                          aria-pressed={selected}
                          aria-label={`ラップ ${lap.number} を地図で表示`}
                          onClick={(event) => {
                            event.stopPropagation()
                            toggle()
                          }}
                        >
                          {lap.number}
                        </button>
                      ) : (
                        lap.number
                      )}
                    </td>
                    <td className="num">{lap.distanceKm.toFixed(2)}</td>
                    <td className="num">{formatDuration(lap.durationSeconds)}</td>
                    <td className="num">{formatPace(lap.paceSecondsPerKm)}</td>
                    <td className="num">
                      {lap.averageHeartRate === null ? '—' : `${lap.averageHeartRate} / ${lap.maxHeartRate ?? '—'}`}
                    </td>
                    <td className="num">{lap.averageCadence ?? '—'}</td>
                    <td className="num">{lap.elevationGainMeters ?? '—'}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )
  }
}
