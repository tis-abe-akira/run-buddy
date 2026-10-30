import { useState } from 'react'
import type { Run } from '../api/client'
import { formatDay, formatDuration, formatKm, formatPace } from '../lib/format'

const COLLAPSED_ROWS = 7

/** Recent runs on a dark product surface. */
export function RunsTable({ runs }: { runs: Run[] }) {
  const [expanded, setExpanded] = useState(false)
  const latestFirst = [...runs].reverse()
  const visible = expanded ? latestFirst : latestFirst.slice(0, COLLAPSED_ROWS)
  return (
    <section className="dark-card stack" aria-labelledby="runs-heading">
      <div className="row">
        <h2 id="runs-heading" className="title-md">
          ランの記録
        </h2>
        <span className="spacer" />
        <span className="caption" style={{ color: 'var(--color-on-dark-soft)' }}>
          {runs.length} 件
        </span>
      </div>
      {runs.length === 0 ? (
        <p className="body-sm" style={{ color: 'var(--color-on-dark-soft)', margin: 0 }}>
          まだ記録がありません。「Garminから取り込む」で今月のランを取得してください。
        </p>
      ) : (
        <div className="table-scroll">
          <table className="table">
            <thead>
              <tr>
                <th>日付</th>
                <th className="num">距離 km</th>
                <th className="num">タイム</th>
                <th className="num">ペース /km</th>
                <th className="num">心拍</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((run) => (
                <tr key={run.activityId}>
                  <td>{formatDay(run.startedAt)}</td>
                  <td className="num">{formatKm(run.distanceKm)}</td>
                  <td className="num">{formatDuration(run.durationSeconds)}</td>
                  <td className="num">{formatPace(run.paceSecondsPerKm)}</td>
                  <td className="num">{run.averageHeartRate ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {runs.length > COLLAPSED_ROWS && (
        <div>
          <button type="button" className="button button-on-dark" onClick={() => setExpanded((v) => !v)} aria-expanded={expanded}>
            {expanded ? '最近の記録だけ表示' : `すべて表示（${runs.length} 件）`}
          </button>
        </div>
      )}
    </section>
  )
}
