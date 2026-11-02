import { useState } from 'react'
import type { Race, RaceInput } from '../api/client'
import { formatDay } from '../lib/format'
import { formatDaysLeft, formatGoal, formatRaceDistance } from '../lib/race'
import { RaceForm } from './RaceForm'

type Props = {
  races: Race[]
  onCreate: (race: RaceInput) => void
  onUpdate: (id: string, race: RaceInput) => void
  onDelete: (id: string) => void
  saving: boolean
  error?: string
}

/** Races the runner trains for: upcoming ones first, then the ones already run. */
export function RacesView({ races, onCreate, onUpdate, onDelete, saving, error }: Props) {
  const upcoming = races.filter((race) => race.daysUntil >= 0)
  const past = races.filter((race) => race.daysUntil < 0).reverse()
  return (
    <div className="grid-plan">
      <div className="stack" style={{ gap: 'var(--spacing-lg)' }}>
        <section className="stack" aria-labelledby="upcoming-races-heading">
          <h2 id="upcoming-races-heading" className="title-md">
            これからのレース
          </h2>
          {upcoming.length === 0 ? (
            <p className="body-sm muted" style={{ margin: 0 }}>
              目標のレースを登録すると、ダッシュボードに残り日数が出て、AI 相談のプラン作りにも使われます。
            </p>
          ) : (
            upcoming.map((race) => (
              <RaceItem key={race.id} race={race} onUpdate={onUpdate} onDelete={onDelete} saving={saving} />
            ))
          )}
        </section>
        {past.length > 0 && (
          <section className="stack" aria-labelledby="past-races-heading">
            <h2 id="past-races-heading" className="title-md muted">
              終わったレース
            </h2>
            {past.map((race) => (
              <RaceItem key={race.id} race={race} onUpdate={onUpdate} onDelete={onDelete} saving={saving} />
            ))}
          </section>
        )}
      </div>
      <section className="feature-card stack" aria-labelledby="new-race-heading">
        <h2 id="new-race-heading" className="title-md">
          レースを登録
        </h2>
        {/* Remount after each list change so a saved race clears the form. */}
        <RaceForm key={races.length} onSubmit={onCreate} saving={saving} />
        {error && (
          <p className="field-error" role="alert" style={{ margin: 0 }}>
            {error}
          </p>
        )}
      </section>
    </div>
  )
}

function RaceItem({
  race,
  onUpdate,
  onDelete,
  saving,
}: {
  race: Race
  onUpdate: Props['onUpdate']
  onDelete: Props['onDelete']
  saving: boolean
}) {
  const [editing, setEditing] = useState(false)
  const [confirming, setConfirming] = useState(false)
  const isPast = race.daysUntil < 0

  if (editing) {
    return (
      <div className="outline-card">
        <RaceForm
          initial={race}
          saving={saving}
          onCancel={() => setEditing(false)}
          onSubmit={(input) => {
            onUpdate(race.id, input)
            setEditing(false)
          }}
        />
      </div>
    )
  }
  return (
    <article className={isPast ? 'outline-card race-item past' : 'outline-card race-item'}>
      <div className="row" style={{ alignItems: 'baseline' }}>
        <h3 className="title-md">{race.name}</h3>
        <span className="spacer" />
        {!isPast && <span className="race-countdown numeric">{formatDaysLeft(race.daysUntil)}</span>}
      </div>
      <p className="body-sm muted" style={{ margin: 0 }}>
        <span className="numeric">{formatDay(race.date)}</span> · <span className="numeric">{formatRaceDistance(race.distanceKm)}</span>
      </p>
      <p className="body-sm" style={{ margin: 0 }}>
        <span className={race.targetSeconds === null ? 'badge-pill' : 'badge-pill numeric'}>{formatGoal(race)}</span>
      </p>
      <div className="row">
        {confirming ? (
          <>
            <span className="body-sm">削除しますか？</span>
            <button type="button" className="button button-secondary" onClick={() => onDelete(race.id)}>
              削除する
            </button>
            <button type="button" className="button button-text" onClick={() => setConfirming(false)}>
              やめる
            </button>
          </>
        ) : (
          <>
            <button type="button" className="button button-secondary" aria-label={`${race.name} を編集`} onClick={() => setEditing(true)}>
              編集
            </button>
            <button type="button" className="button button-text" aria-label={`${race.name} を削除`} onClick={() => setConfirming(true)}>
              削除
            </button>
          </>
        )}
      </div>
    </article>
  )
}
