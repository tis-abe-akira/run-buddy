import type { Race } from '../api/client'
import { formatDay } from '../lib/format'
import { formatDaysLeft, formatGoal, formatRaceDistance } from '../lib/race'

/** The next race on the dashboard: how long is left and what the runner is aiming for. */
export function NextRaceCard({ race }: { race: Race }) {
  return (
    <section className="outline-card next-race" aria-label="次のレース">
      <span className="caption-uppercase muted">Next race</span>
      <div className="row" style={{ alignItems: 'baseline', gap: 'var(--spacing-md)' }}>
        <span className="display-sm">{race.name}</span>
        <span className="body-sm muted numeric">
          {formatDay(race.date)} · {formatRaceDistance(race.distanceKm)}
        </span>
      </div>
      <div className="row" style={{ alignItems: 'baseline', gap: 'var(--spacing-md)' }}>
        <span className="display-md numeric">{formatDaysLeft(race.daysUntil)}</span>
        <span className="badge-pill numeric">{formatGoal(race)}</span>
      </div>
    </section>
  )
}
