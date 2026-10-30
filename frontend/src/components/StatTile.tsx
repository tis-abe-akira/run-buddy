import type { ReactNode } from 'react'

type Props = {
  label: string
  value: string
  unit?: string
  note?: ReactNode
}

/** A headline number on a cream feature-card. */
export function StatTile({ label, value, unit, note }: Props) {
  return (
    <section className="feature-card stat-tile" aria-label={label}>
      <div className="caption-uppercase muted">{label}</div>
      <div className="display-md numeric stat-value">
        {value}
        {unit && <span className="stat-unit">{unit}</span>}
      </div>
      {note && <div className="body-sm muted">{note}</div>}
    </section>
  )
}
