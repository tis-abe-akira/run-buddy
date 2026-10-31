import { Link } from '@tanstack/react-router'
import type { Dashboard, ImportResult, Run } from '../api/client'
import { formatKm, formatPace, formatRate } from '../lib/format'
import { formatMonth, shiftMonth, type Month } from '../lib/month'
import { MonthSwitcher } from './MonthSwitcher'
import { ProgressChart } from './ProgressChart'
import { RunsTable } from './RunsTable'
import { StatTile } from './StatTile'

type Props = {
  dashboard: Dashboard
  runs: Run[]
  /** Days of the month that have already happened (0 for a future month). */
  elapsedDays: number
  onMonthChange: (month: Month) => void
  onImport: () => void
  importing: boolean
  lastImport?: ImportResult
  importError?: string
}

export function DashboardView({ dashboard, runs, elapsedDays, onMonthChange, onImport, importing, lastImport, importError }: Props) {
  const month = dashboard.month
  const remainingKm = Math.max(dashboard.targetKm - dashboard.actualKm, 0)

  return (
    <>
      {/* hero-band: cream canvas */}
      <section className="band">
        <div className="container stack" style={{ gap: 'var(--spacing-lg)' }}>
          <div className="row" style={{ alignItems: 'flex-end', gap: 'var(--spacing-lg)' }}>
            <div className="stack" style={{ gap: 'var(--spacing-xs)' }}>
              <span className="caption-uppercase muted">Monthly review</span>
              <h1 className="display-lg">{formatMonth(month)}の走り</h1>
            </div>
            <span className="spacer" />
            <MonthSwitcher month={month} onChange={onMonthChange} />
          </div>
          <div className="row">
            <button type="button" className="button button-primary" onClick={onImport} disabled={importing}>
              {importing ? '取り込み中…' : 'Garminから取り込む'}
            </button>
            <span className="badge-pill">ダミーデータ</span>
            {lastImport && !importing && (
              <span className="body-sm muted" role="status">
                {lastImport.fetched} 件を確認し、新しく {lastImport.added} 件を取り込みました
              </span>
            )}
            {importError && <span className="field-error" role="alert">{importError}</span>}
          </div>
        </div>
      </section>

      {/* stat tiles: cream cards */}
      <section className="band">
        <div className="container grid-4">
          <StatTile
            label="実績距離"
            value={formatKm(dashboard.actualKm)}
            unit="km"
            note={dashboard.targetKm > 0 ? `目標 ${formatKm(dashboard.targetKm)} km まであと ${formatKm(remainingKm)} km` : '目標は未設定です'}
          />
          <StatTile label="目標達成率" value={formatRate(dashboard.achievementRate)} note={dashboard.targetKm > 0 ? '実績 ÷ 目標' : 'プランで目標を設定しましょう'} />
          <StatTile
            label="ラン回数"
            value={String(dashboard.runCount)}
            unit="回"
            note={dashboard.plannedWorkoutCount > 0 ? `予定 ${dashboard.plannedWorkoutCount} 回のうち ${dashboard.completedWorkoutCount} 回を実施` : '予定ワークアウトなし'}
          />
          <StatTile label="平均ペース" value={formatPace(dashboard.averagePaceSecondsPerKm)} unit="/km" note="総時間 ÷ 総距離" />
        </div>
      </section>

      {/* chart: cream canvas with hairline */}
      <section className="band">
        <div className="container">
          <ProgressChart days={dashboard.daily} targetKm={dashboard.targetKm} elapsedDays={elapsedDays} />
        </div>
      </section>

      {/* runs: dark product surface */}
      <section className="band">
        <div className="container">
          <RunsTable runs={runs} />
        </div>
      </section>

      {/* callout: coral */}
      <section className="band">
        <div className="container">
          <div className="callout-coral row" style={{ gap: 'var(--spacing-lg)' }}>
            <div className="stack" style={{ gap: 'var(--spacing-xs)', flex: '1 1 320px' }}>
              <h2 className="display-sm">次の一か月を、計画から。</h2>
              <p style={{ margin: 0 }}>今月の走りを踏まえて、{formatMonth(shiftMonth(month, 1))}のプランを AI と一緒に立てましょう。</p>
            </div>
            <div className="row">
              <Link to="/advisor" search={{ month: shiftMonth(month, 1) }} className="button button-on-coral">
                AIに相談する
              </Link>
              <Link to="/plans/$month" params={{ month: shiftMonth(month, 1) }} className="button button-text" style={{ color: 'var(--color-on-primary)' }}>
                自分で作る
              </Link>
            </div>
          </div>
        </div>
      </section>
    </>
  )
}
