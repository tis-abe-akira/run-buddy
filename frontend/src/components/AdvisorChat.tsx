import { useState, type FormEvent } from 'react'
import type { AdvisorInfo, ProposedPlan } from '../api/client'
import { formatKm } from '../lib/format'
import { isWeekend } from '../lib/weekday'
import { DayLabel } from './DayLabel'
import { formatMonth, type Month } from '../lib/month'
import { WORKOUT_TYPES } from './PlanEditor'

export type ChatEntry =
  | { speaker: 'USER'; text: string }
  | { speaker: 'ADVISOR'; text: string; provider: string; model: string; proposal: ProposedPlan | null }

export type Adoption = { index: number; state: 'saving' | 'saved' | 'error'; message?: string }

type Props = {
  month: Month
  providers: AdvisorInfo[]
  provider: string
  model: string
  onChoose: (provider: string, model: string) => void
  entries: ChatEntry[]
  pending: boolean
  error?: string
  onSend: (text: string) => void
  onAdopt: (index: number) => void
  adoption?: Adoption
  onReset: () => void
}

const SUGGESTIONS = ['先月の走りを振り返って', 'プランを作って', '週末にロング走を入れたい']

/** A conversation with the advisor. The provider and model can be changed before any message. */
export function AdvisorChat(props: Props) {
  const { month, providers, provider, model, onChoose, entries, pending, error, onSend, onAdopt, adoption, onReset } = props
  const [draft, setDraft] = useState('')
  const models = providers.find((p) => p.id === provider)?.models ?? []

  function send(text: string) {
    const trimmed = text.trim()
    if (trimmed === '' || pending) {
      return
    }
    onSend(trimmed)
    setDraft('')
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    send(draft)
  }

  return (
    <section className="outline-card stack chat" aria-label={`${formatMonth(month)}のプラン相談`}>
      <div className="row">
        <label className="field">
          <span className="field-label">プロバイダ</span>
          <select
            className="text-input"
            value={provider}
            onChange={(e) => {
              const next = providers.find((p) => p.id === e.target.value)
              if (next) {
                onChoose(next.id, next.models[0]!)
              }
            }}
          >
            {providers.map((p) => (
              <option key={p.id} value={p.id}>
                {p.displayName}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          <span className="field-label">モデル</span>
          <select className="text-input" value={model} onChange={(e) => onChoose(provider, e.target.value)}>
            {models.map((m) => (
              <option key={m} value={m}>
                {m}
              </option>
            ))}
          </select>
        </label>
        <span className="spacer" />
        {entries.length > 0 && (
          <button type="button" className="button button-secondary" onClick={onReset}>
            新しい相談
          </button>
        )}
      </div>

      <ol className="chat-log" aria-live="polite">
        {entries.length === 0 && (
          <li className="chat-empty muted body-sm">
            {formatMonth(month)}のプランについて相談できます。AI があなたのランの記録を確認してから答えます。
          </li>
        )}
        {entries.map((entry, index) =>
          entry.speaker === 'USER' ? (
            <li key={index} className="chat-message chat-user">
              {entry.text}
            </li>
          ) : (
            <li key={index} className="chat-message chat-advisor">
              <div className="caption muted">
                {providers.find((p) => p.id === entry.provider)?.displayName ?? entry.provider} · {entry.model}
              </div>
              <p className="chat-text">{entry.text}</p>
              {entry.proposal && (
                <ProposalCard proposal={entry.proposal} adoption={adoption?.index === index ? adoption : undefined} onAdopt={() => onAdopt(index)} />
              )}
            </li>
          ),
        )}
        {pending && (
          <li className="chat-message chat-advisor muted" role="status">
            考えています…
          </li>
        )}
      </ol>

      {error && (
        <div className="form-error" role="alert">
          {error}
        </div>
      )}

      <div className="row" aria-label="よくある相談">
        {SUGGESTIONS.map((s) => (
          <button key={s} type="button" className="badge-pill chip" onClick={() => send(s)} disabled={pending}>
            {s}
          </button>
        ))}
      </div>
      <form className="chat-composer" onSubmit={handleSubmit}>
        <textarea
          className="text-input"
          rows={2}
          aria-label="相談内容"
          placeholder="例: 12月のレースに向けて、11月はどれくらい走ればいい？"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
        />
        <button type="submit" className="button button-primary" disabled={pending || draft.trim() === ''}>
          送信
        </button>
      </form>
      <p className="caption muted-soft" style={{ margin: 0 }}>
        相談内容とランの記録（距離・ペース・心拍数など）は、選択したプロバイダに送信されます。
      </p>
    </section>
  )
}

const COLLAPSED_WORKOUTS = 7

function ProposalCard({ proposal, adoption, onAdopt }: { proposal: ProposedPlan; adoption?: Adoption; onAdopt: () => void }) {
  const [expanded, setExpanded] = useState(false)
  const visible = expanded ? proposal.workouts : proposal.workouts.slice(0, COLLAPSED_WORKOUTS)
  const plannedKm = proposal.workouts.reduce((sum, w) => sum + w.distanceKm, 0)
  const typeLabel = (type: string) => WORKOUT_TYPES.find((t) => t.value === type)?.label ?? type
  return (
    <div className="dark-card stack proposal" aria-label="プラン案">
      <div className="row">
        <span className="title-md">{formatMonth(proposal.month)}のプラン案</span>
        <span className="spacer" />
        <span className="numeric" style={{ color: 'var(--color-on-dark-soft)' }}>
          目標 {formatKm(proposal.targetKm)} km · 予定 {proposal.workouts.length} 回 / {formatKm(plannedKm)} km
        </span>
      </div>
      <div className="table-scroll">
        <table className="table">
          <thead>
            <tr>
              <th>日付</th>
              <th>種別</th>
              <th className="num">距離 km</th>
              <th>メモ</th>
            </tr>
          </thead>
          <tbody>
            {visible.map((w) => (
              <tr key={w.date + w.type} className={isWeekend(w.date) ? 'weekend-row' : undefined}>
                <td>
                  <DayLabel date={w.date} />
                </td>
                <td>{typeLabel(w.type)}</td>
                <td className="num">{formatKm(w.distanceKm)}</td>
                <td>{w.note}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {proposal.workouts.length > COLLAPSED_WORKOUTS && (
        <div>
          <button type="button" className="button button-on-dark" onClick={() => setExpanded((v) => !v)} aria-expanded={expanded}>
            {expanded ? '最初の数件だけ表示' : `すべて表示（${proposal.workouts.length} 件）`}
          </button>
        </div>
      )}
      <div className="row">
        <button type="button" className="button button-primary" onClick={onAdopt} disabled={adoption?.state === 'saving' || adoption?.state === 'saved'}>
          {adoption?.state === 'saving' ? '保存中…' : adoption?.state === 'saved' ? '採用しました' : 'このプランを採用'}
        </button>
        <span className="caption" style={{ color: 'var(--color-on-dark-soft)' }}>
          {adoption?.state === 'error'
            ? `保存できませんでした: ${adoption.message ?? ''}`
            : `採用すると${formatMonth(proposal.month)}の今のプランは置き換えられます。`}
        </span>
      </div>
    </div>
  )
}
