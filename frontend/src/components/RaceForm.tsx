import { useState, type FormEvent } from 'react'
import type { Race, RaceInput } from '../api/client'
import { DISTANCE_PRESETS, formatFinishTime, parseFinishTime } from '../lib/race'

const OTHER = 'その他'

type Props = {
  /** The race being edited; a new race when absent. */
  initial?: Race
  onSubmit: (race: RaceInput) => void
  onCancel?: () => void
  saving: boolean
}

type Errors = Partial<Record<'name' | 'date' | 'distance' | 'target', string>>

export function RaceForm({ initial, onSubmit, onCancel, saving }: Props) {
  const preset = initial ? DISTANCE_PRESETS.find((p) => p.km === initial.distanceKm) : DISTANCE_PRESETS[0]
  const [name, setName] = useState(initial?.name ?? '')
  const [date, setDate] = useState(initial?.date ?? '')
  const [distanceChoice, setDistanceChoice] = useState(preset?.label ?? OTHER)
  const [customKm, setCustomKm] = useState(preset ? '' : String(initial?.distanceKm ?? ''))
  const [target, setTarget] = useState(initial?.targetSeconds != null ? formatFinishTime(initial.targetSeconds) : '')
  const [errors, setErrors] = useState<Errors>({})

  const submit = (event: FormEvent) => {
    event.preventDefault()
    const found: Errors = {}
    if (name.trim() === '') found.name = '大会名を入力してください'
    if (date === '') found.date = '日付を入力してください'
    const km = distanceChoice === OTHER ? Number(customKm) : DISTANCE_PRESETS.find((p) => p.label === distanceChoice)!.km
    if (!(km > 0)) found.distance = '距離を km で入力してください'
    const finish = parseFinishTime(target)
    if (!finish.ok) found.target = '目標タイムは 5:00:00 や 4:30 の形で入力してください'
    setErrors(found)
    if (Object.keys(found).length > 0 || !finish.ok) {
      return
    }
    onSubmit({ name: name.trim(), date, distanceKm: km, targetSeconds: finish.seconds })
  }

  const id = initial?.id ?? 'new'
  return (
    <form className="race-form" onSubmit={submit} noValidate>
      <Field id={`race-name-${id}`} label="大会名" error={errors.name}>
        <input id={`race-name-${id}`} className="text-input" value={name} maxLength={100} onChange={(e) => setName(e.target.value)} />
      </Field>
      <Field id={`race-date-${id}`} label="日付" error={errors.date}>
        <input id={`race-date-${id}`} type="date" className="text-input" value={date} onChange={(e) => setDate(e.target.value)} />
      </Field>
      <Field id={`race-distance-${id}`} label="距離" error={distanceChoice === OTHER ? undefined : errors.distance}>
        <select
          id={`race-distance-${id}`}
          className="text-input"
          value={distanceChoice}
          onChange={(e) => setDistanceChoice(e.target.value)}
        >
          {DISTANCE_PRESETS.map((p) => (
            <option key={p.label}>{p.label}</option>
          ))}
          <option>{OTHER}</option>
        </select>
      </Field>
      {distanceChoice === OTHER && (
        <Field id={`race-km-${id}`} label="距離（km）" error={errors.distance}>
          <input
            id={`race-km-${id}`}
            className="text-input numeric"
            inputMode="decimal"
            value={customKm}
            onChange={(e) => setCustomKm(e.target.value)}
          />
        </Field>
      )}
      <Field id={`race-target-${id}`} label="目標タイム（任意）" error={errors.target}>
        <input
          id={`race-target-${id}`}
          className="text-input numeric"
          placeholder="5:00:00（空欄なら完走が目標）"
          value={target}
          onChange={(e) => setTarget(e.target.value)}
        />
      </Field>
      <div className="row">
        <button type="submit" className="button button-primary" disabled={saving}>
          {initial ? '保存する' : '登録する'}
        </button>
        {onCancel && (
          <button type="button" className="button button-secondary" onClick={onCancel}>
            キャンセル
          </button>
        )}
      </div>
    </form>
  )
}

function Field({ id, label, error, children }: { id: string; label: string; error?: string; children: React.ReactNode }) {
  return (
    <div className="stack" style={{ gap: 'var(--spacing-xxs)' }}>
      <label htmlFor={id} className="caption">
        {label}
      </label>
      {children}
      {error && <span className="field-error">{error}</span>}
    </div>
  )
}
