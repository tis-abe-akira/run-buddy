import { useState, type FormEvent } from 'react'
import type { Plan, WorkoutType } from '../api/client'
import { formatKm } from '../lib/format'
import { containsDate, formatMonth, type Month } from '../lib/month'

export const WORKOUT_TYPES: { value: WorkoutType; label: string }[] = [
  { value: 'EASY', label: 'イージー' },
  { value: 'TEMPO', label: 'テンポ走' },
  { value: 'INTERVAL', label: 'インターバル' },
  { value: 'LONG', label: 'ロング走' },
  { value: 'RACE', label: 'レース' },
]

type Props = {
  month: Month
  initial: Plan
  onSave: (plan: Plan) => void
  saving: boolean
  saved: boolean
  serverError?: string
}

/** Text fields stay strings while editing, so a half-typed value is never coerced. */
type DraftWorkout = { key: number; date: string; type: WorkoutType; distanceKm: string; note: string }
type Draft = { targetKm: string; workouts: DraftWorkout[] }
type Errors = { targetKm?: string; workouts: Record<number, string> }

let nextKey = 0

function toDraft(plan: Plan): Draft {
  return {
    targetKm: plan.targetKm > 0 ? String(plan.targetKm) : '',
    workouts: plan.workouts.map((w) => ({ key: nextKey++, date: w.date, type: w.type, distanceKm: String(w.distanceKm), note: w.note })),
  }
}

export function validate(month: Month, draft: Draft): Errors {
  const errors: Errors = { workouts: {} }
  const target = draft.targetKm === '' ? 0 : Number(draft.targetKm)
  if (!Number.isFinite(target) || target < 0) {
    errors.targetKm = '0以上の数値を入力してください'
  }
  for (const workout of draft.workouts) {
    const distance = Number(workout.distanceKm)
    if (!containsDate(month, workout.date)) {
      errors.workouts[workout.key] = `${formatMonth(month)}の日付を選んでください`
    } else if (workout.distanceKm === '' || !Number.isFinite(distance) || distance <= 0) {
      errors.workouts[workout.key] = '距離は0より大きい数値にしてください'
    }
  }
  return errors
}

function hasErrors(errors: Errors) {
  return errors.targetKm !== undefined || Object.keys(errors.workouts).length > 0
}

export function PlanEditor({ month, initial, onSave, saving, saved, serverError }: Props) {
  const [draft, setDraft] = useState<Draft>(() => toDraft(initial))
  const [errors, setErrors] = useState<Errors>({ workouts: {} })

  const plannedKm = draft.workouts.reduce((sum, w) => sum + (Number(w.distanceKm) || 0), 0)
  const targetKm = Number(draft.targetKm) || 0

  function updateWorkout(key: number, patch: Partial<DraftWorkout>) {
    setDraft((d) => ({ ...d, workouts: d.workouts.map((w) => (w.key === key ? { ...w, ...patch } : w)) }))
  }

  function addWorkout() {
    const lastDate = draft.workouts[draft.workouts.length - 1]?.date
    setDraft((d) => ({
      ...d,
      workouts: [...d.workouts, { key: nextKey++, date: lastDate ?? `${month}-01`, type: 'EASY', distanceKm: '', note: '' }],
    }))
  }

  function removeWorkout(key: number) {
    setDraft((d) => ({ ...d, workouts: d.workouts.filter((w) => w.key !== key) }))
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const found = validate(month, draft)
    setErrors(found)
    if (hasErrors(found)) {
      return
    }
    onSave({
      targetKm,
      workouts: draft.workouts.map((w) => ({ date: w.date, type: w.type, distanceKm: Number(w.distanceKm), note: w.note })),
    })
  }

  return (
    <form className="grid-plan" onSubmit={handleSubmit} noValidate aria-label={`${formatMonth(month)}のプラン`}>
      <section className="outline-card stack" aria-labelledby="workouts-heading">
        <h2 id="workouts-heading" className="title-md">
          予定ワークアウト
        </h2>
        {draft.workouts.length === 0 && <p className="body-sm muted" style={{ margin: 0 }}>まだ予定がありません。「ワークアウトを追加」から入力してください。</p>}
        <div className="stack workout-list" style={{ gap: 'var(--spacing-sm)' }}>
        {draft.workouts.map((workout, index) => {
          const error = errors.workouts[workout.key]
          const n = index + 1
          return (
            <div key={workout.key} className="stack" style={{ gap: 'var(--spacing-xxs)' }}>
              <div className={index === 0 ? 'workout-row' : 'workout-row repeat'}>
                <label className="field">
                  <span className="field-label">日付</span>
                  <input
                    type="date"
                    className="text-input"
                    aria-label={`${n}件目の日付`}
                    min={`${month}-01`}
                    max={`${month}-31`}
                    value={workout.date}
                    aria-invalid={error !== undefined}
                    onChange={(e) => updateWorkout(workout.key, { date: e.target.value })}
                  />
                </label>
                <label className="field">
                  <span className="field-label">種別</span>
                  <select
                    className="text-input"
                    aria-label={`${n}件目の種別`}
                    value={workout.type}
                    onChange={(e) => updateWorkout(workout.key, { type: e.target.value as WorkoutType })}
                  >
                    {WORKOUT_TYPES.map((t) => (
                      <option key={t.value} value={t.value}>
                        {t.label}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="field">
                  <span className="field-label">距離 km</span>
                  <input
                    type="number"
                    inputMode="decimal"
                    step="0.1"
                    min="0"
                    className="text-input numeric"
                    aria-label={`${n}件目の距離`}
                    value={workout.distanceKm}
                    aria-invalid={error !== undefined}
                    onChange={(e) => updateWorkout(workout.key, { distanceKm: e.target.value })}
                  />
                </label>
                <label className="field">
                  <span className="field-label">メモ</span>
                  <input
                    type="text"
                    className="text-input"
                    aria-label={`${n}件目のメモ`}
                    value={workout.note}
                    placeholder="例: 400m × 10"
                    onChange={(e) => updateWorkout(workout.key, { note: e.target.value })}
                  />
                </label>
                <button type="button" className="button-icon" aria-label={`${n}件目を削除`} onClick={() => removeWorkout(workout.key)}>
                  ×
                </button>
              </div>
              {error && <span className="field-error">{error}</span>}
            </div>
          )
        })}
        </div>
        <div>
          <button type="button" className="button button-secondary" onClick={addWorkout}>
            ＋ ワークアウトを追加
          </button>
        </div>
      </section>

      <aside className="feature-card stack plan-summary" aria-label="プランの概要">
        <label className="field">
          <span className="field-label">月間目標 km</span>
          <input
            type="number"
            inputMode="decimal"
            step="1"
            min="0"
            className="text-input numeric"
            value={draft.targetKm}
            placeholder="例: 150"
            aria-invalid={errors.targetKm !== undefined}
            onChange={(e) => setDraft((d) => ({ ...d, targetKm: e.target.value }))}
          />
          {errors.targetKm && <span className="field-error">{errors.targetKm}</span>}
        </label>
        <div>
          <div className="caption-uppercase muted">予定の合計</div>
          <div className="display-md numeric">
            {formatKm(plannedKm)}
            <span className="stat-unit">km</span>
          </div>
          <div className="body-sm muted">
            {draft.workouts.length} 回
            {targetKm > 0 && `・目標の ${Math.round((plannedKm / targetKm) * 100)}%`}
          </div>
        </div>
        {serverError && (
          <div className="form-error" role="alert">
            保存できませんでした: {serverError}
          </div>
        )}
        <div className="row">
          <button type="submit" className="button button-primary" disabled={saving}>
            {saving ? '保存中…' : 'プランを保存'}
          </button>
          {saved && !saving && (
            <span className="body-sm muted" role="status">
              保存しました
            </span>
          )}
        </div>
      </aside>
    </form>
  )
}
