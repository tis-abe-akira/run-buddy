import type { DayDetail, Run } from '../api/client'
import { formatStartTime } from '../lib/day'
import { formatDuration, formatKm, formatPace } from '../lib/format'
import { workoutTypeLabel } from '../lib/workout'

/**
 * One day: the plan, then every run of the day. Most days have one run, so this reads as the run's
 * detail page; a day with a morning and an evening run stacks them, numbered in the order they started.
 */
export function DayDetailView({ day }: { day: DayDetail }) {
  return (
    <div className="stack" style={{ gap: 'var(--spacing-lg)' }}>
      <section className="feature-card stack" aria-labelledby="day-plan-heading" style={{ gap: 'var(--spacing-sm)' }}>
        <h2 id="day-plan-heading" className="title-md">
          予定
        </h2>
        {day.plannedWorkouts.length === 0 ? (
          <p className="body-sm muted" style={{ margin: 0 }}>
            予定はありません
          </p>
        ) : (
          <ul className="day-plan-list">
            {day.plannedWorkouts.map((workout, i) => (
              <li key={i} className="row" style={{ gap: 'var(--spacing-sm)' }}>
                <span className="badge-pill">{workoutTypeLabel(workout.type)}</span>
                <span className="numeric">{formatKm(workout.distanceKm)} km</span>
                {workout.note && <span className="body-sm muted">{workout.note}</span>}
              </li>
            ))}
          </ul>
        )}
      </section>

      {day.runs.length === 0 ? (
        <p className="body-sm muted">この日のランはありません</p>
      ) : (
        day.runs.map((run, i) => (
          <RunDetail key={run.activityId} run={run} number={day.runs.length > 1 ? i + 1 : undefined} />
        ))
      )}
    </div>
  )
}

function RunDetail({ run, number }: { run: Run; number: number | undefined }) {
  const title = `${number ? `ラン ${number} · ` : ''}${formatStartTime(run.startedAt)} スタート`
  return (
    <section className="outline-card stack" aria-label={title} style={{ gap: 'var(--spacing-lg)' }}>
      <h2 className="title-md">{title}</h2>
      <div className="run-headline">
        <Headline label="距離" value={formatKm(run.distanceKm)} unit="km" />
        <Headline label="タイム" value={formatDuration(run.durationSeconds)} />
        <Headline label="ペース" value={formatPace(run.paceSecondsPerKm)} unit="/km" />
      </div>
      <dl className="metric-grid">
        <Measurement label="心拍 平均" value={run.averageHeartRate} unit="bpm" />
        <Measurement label="心拍 最大" value={run.maxHeartRate} unit="bpm" />
        <Measurement label="ケイデンス 平均" value={run.averageCadence} unit="spm" />
        <Measurement label="ケイデンス 最大" value={run.maxCadence} unit="spm" />
        <Measurement label="歩幅" value={run.averageStrideLengthCm} unit="cm" />
        <Measurement label="上昇" value={run.elevationGainMeters} unit="m" />
      </dl>
    </section>
  )
}

function Headline({ label, value, unit }: { label: string; value: string; unit?: string }) {
  return (
    <div className="stack" style={{ gap: 'var(--spacing-xxs)' }}>
      <span className="caption muted">{label}</span>
      <span className="display-md numeric">
        {value}
        {unit && <span className="stat-unit">{unit}</span>}
      </span>
    </div>
  )
}

function Measurement({ label, value, unit }: { label: string; value: number | null; unit: string }) {
  return (
    <div>
      <dt className="caption muted">{label}</dt>
      <dd className="numeric">
        {value === null ? (
          '—'
        ) : (
          <>
            {value}
            <span className="metric-unit">{unit}</span>
          </>
        )}
      </dd>
    </div>
  )
}
