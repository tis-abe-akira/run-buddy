import { useLayoutEffect, useRef, useState, type PointerEvent } from 'react'
import type { DashboardDay } from '../api/client'
import { formatDay, formatKm } from '../lib/format'

type Props = {
  days: DashboardDay[]
  targetKm: number
  /** Number of leading days that have happened; the actual line stops there. */
  elapsedDays: number
}

const HEIGHT = 280
const MARGIN = { top: 16, right: 112, bottom: 28, left: 44 }
const LABEL_MIN_GAP = 18

/**
 * Month-to-date distance: actual (solid coral) against planned (dashed teal), with the monthly
 * target as a reference line. One y-axis only.
 */
export function ProgressChart({ days, targetKm, elapsedDays }: Props) {
  const [containerRef, width] = useElementWidth<HTMLDivElement>(720)
  const [hoverIndex, setHoverIndex] = useState<number | null>(null)
  const [showTable, setShowTable] = useState(false)

  const lastDay = days[days.length - 1]
  if (!lastDay) {
    return null
  }
  const hasPlan = lastDay.cumulativePlannedKm > 0
  const actualDays = days.slice(0, Math.max(0, Math.min(elapsedDays, days.length)))
  const lastActual = actualDays[actualDays.length - 1]

  const plotWidth = Math.max(width - MARGIN.left - MARGIN.right, 120)
  const plotHeight = HEIGHT - MARGIN.top - MARGIN.bottom
  const yTicks = niceTicks(Math.max(targetKm, lastDay.cumulativePlannedKm, lastActual?.cumulativeActualKm ?? 0, 10))
  const yMax = yTicks[yTicks.length - 1] ?? 10
  const x = (index: number) => MARGIN.left + (days.length === 1 ? 0 : (index / (days.length - 1)) * plotWidth)
  const y = (km: number) => MARGIN.top + plotHeight - (km / yMax) * plotHeight

  const plannedPath = linePath(days.map((d, i) => [x(i), y(d.cumulativePlannedKm)]))
  const actualPoints = actualDays.map((d, i): [number, number] => [x(i), y(d.cumulativeActualKm)])
  const actualPath = linePath(actualPoints)
  const actualArea =
    actualPoints.length > 1
      ? `${actualPath}L${x(actualPoints.length - 1)},${y(0)}L${x(0)},${y(0)}Z`
      : ''

  const endLabels = placeEndLabels([
    ...(lastActual
      ? [{ key: 'actual', y: y(lastActual.cumulativeActualKm), text: `実績 ${formatKm(lastActual.cumulativeActualKm)}` }]
      : []),
    ...(hasPlan ? [{ key: 'planned', y: y(lastDay.cumulativePlannedKm), text: `計画 ${formatKm(lastDay.cumulativePlannedKm)}` }] : []),
  ])

  const xTickEvery = plotWidth < 320 ? 10 : 5
  const xTickIndexes = days.map((_, i) => i).filter((i) => i === 0 || (i + 1) % xTickEvery === 0)

  function handlePointerMove(event: PointerEvent<SVGRectElement>) {
    const box = event.currentTarget.getBoundingClientRect()
    const ratio = (event.clientX - box.left) / box.width
    setHoverIndex(Math.round(ratio * (days.length - 1)))
  }

  const hovered = hoverIndex === null ? undefined : days[hoverIndex]
  const hoveredHasActual = hoverIndex !== null && hoverIndex < actualDays.length

  return (
    <section className="outline-card stack" aria-labelledby="progress-heading">
      <div className="row">
        <h2 id="progress-heading" className="title-md">
          累積距離
        </h2>
        <span className="spacer" />
        <div className="chart-legend" aria-label="凡例">
          <LegendItem label="実績" color="var(--chart-actual)" />
          {hasPlan && <LegendItem label="計画" color="var(--chart-planned)" dashed />}
          {targetKm > 0 && <LegendItem label="目標" color="var(--color-muted-soft)" thin />}
        </div>
      </div>

      <div ref={containerRef} style={{ position: 'relative' }}>
        <svg
          width={width}
          height={HEIGHT}
          role="img"
          aria-label={`累積距離のグラフ。実績 ${formatKm(lastActual?.cumulativeActualKm ?? 0)} km`}
          style={{ display: 'block', overflow: 'visible' }}
        >
          {yTicks.map((tick) => (
            <g key={tick}>
              <line x1={MARGIN.left} x2={MARGIN.left + plotWidth} y1={y(tick)} y2={y(tick)} stroke="var(--chart-grid)" strokeWidth={1} />
              <text x={MARGIN.left - 8} y={y(tick)} dy="0.32em" textAnchor="end" fontSize={12} fill="var(--color-muted)">
                {tick}
              </text>
            </g>
          ))}
          {xTickIndexes.map((i) => (
            <text key={i} x={x(i)} y={HEIGHT - 8} textAnchor="middle" fontSize={12} fill="var(--color-muted)">
              {i + 1}日
            </text>
          ))}

          {targetKm > 0 && (
            <g>
              <line
                x1={MARGIN.left}
                x2={MARGIN.left + plotWidth}
                y1={y(targetKm)}
                y2={y(targetKm)}
                stroke="var(--color-muted-soft)"
                strokeWidth={1}
              />
              <text x={MARGIN.left + 4} y={y(targetKm) - 6} fontSize={12} fill="var(--color-muted)">
                目標 {formatKm(targetKm)} km
              </text>
            </g>
          )}

          {actualArea && <path d={actualArea} fill="var(--chart-actual)" opacity={0.1} />}
          {hasPlan && (
            <path d={plannedPath} fill="none" stroke="var(--chart-planned)" strokeWidth={2} strokeDasharray="6 4" strokeLinecap="round" strokeLinejoin="round" />
          )}
          {actualPoints.length > 0 && (
            <path d={actualPath} fill="none" stroke="var(--chart-actual)" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" />
          )}

          {lastActual && <Dot cx={x(actualDays.length - 1)} cy={y(lastActual.cumulativeActualKm)} color="var(--chart-actual)" />}
          {hasPlan && <Dot cx={x(days.length - 1)} cy={y(lastDay.cumulativePlannedKm)} color="var(--chart-planned)" />}

          {endLabels.map((label) => (
            <text
              key={label.key}
              x={MARGIN.left + plotWidth + 12}
              y={label.y}
              dy="0.32em"
              fontSize={13}
              fontWeight={500}
              fill="var(--color-ink)"
            >
              {label.text}
              <tspan fill="var(--color-muted)" fontWeight={400}> km</tspan>
            </text>
          ))}

          {hovered && hoverIndex !== null && (
            <g pointerEvents="none">
              <line x1={x(hoverIndex)} x2={x(hoverIndex)} y1={MARGIN.top} y2={MARGIN.top + plotHeight} stroke="var(--color-muted-soft)" strokeWidth={1} />
              {hasPlan && <Dot cx={x(hoverIndex)} cy={y(hovered.cumulativePlannedKm)} color="var(--chart-planned)" />}
              {hoveredHasActual && <Dot cx={x(hoverIndex)} cy={y(hovered.cumulativeActualKm)} color="var(--chart-actual)" />}
            </g>
          )}

          <rect
            x={MARGIN.left}
            y={MARGIN.top}
            width={plotWidth}
            height={plotHeight}
            fill="transparent"
            onPointerMove={handlePointerMove}
            onPointerLeave={() => setHoverIndex(null)}
          />
        </svg>

        {hovered && hoverIndex !== null && (
          <div className="chart-tooltip" style={{ left: x(hoverIndex), top: MARGIN.top + 8 }} role="status">
            <div className="caption">{formatDay(hovered.date)}</div>
            {hoveredHasActual && (
              <div>
                <span className="swatch" style={{ background: 'var(--chart-actual)' }} />
                実績 {formatKm(hovered.cumulativeActualKm)} km（この日 {formatKm(hovered.actualKm)}）
              </div>
            )}
            {hasPlan && (
              <div>
                <span className="swatch" style={{ background: 'var(--chart-planned)' }} />
                計画 {formatKm(hovered.cumulativePlannedKm)} km（この日 {formatKm(hovered.plannedKm)}）
              </div>
            )}
          </div>
        )}
      </div>

      <div>
        <button type="button" className="button button-text" onClick={() => setShowTable((v) => !v)} aria-expanded={showTable}>
          {showTable ? '表を閉じる' : '表で見る'}
        </button>
      </div>
      {showTable && <DailyTable days={days} elapsedDays={actualDays.length} />}
    </section>
  )
}

function DailyTable({ days, elapsedDays }: { days: DashboardDay[]; elapsedDays: number }) {
  const rows = days.filter((d, i) => d.plannedKm > 0 || (i < elapsedDays && d.actualKm > 0))
  return (
    <div className="table-scroll">
      <table className="table">
        <thead>
          <tr>
            <th>日付</th>
            <th className="num">計画 km</th>
            <th className="num">実績 km</th>
            <th className="num">計画 累積</th>
            <th className="num">実績 累積</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((d) => (
            <tr key={d.date}>
              <td>{formatDay(d.date)}</td>
              <td className="num">{formatKm(d.plannedKm)}</td>
              <td className="num">{formatKm(d.actualKm)}</td>
              <td className="num">{formatKm(d.cumulativePlannedKm)}</td>
              <td className="num">{formatKm(d.cumulativeActualKm)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function LegendItem({ label, color, dashed = false, thin = false }: { label: string; color: string; dashed?: boolean; thin?: boolean }) {
  return (
    <span className="chart-legend-item">
      <svg width="20" height="8" aria-hidden="true">
        <line x1="1" x2="19" y1="4" y2="4" stroke={color} strokeWidth={thin ? 1 : 2} strokeDasharray={dashed ? '5 3' : undefined} strokeLinecap="round" />
      </svg>
      {label}
    </span>
  )
}

function Dot({ cx, cy, color }: { cx: number; cy: number; color: string }) {
  return <circle cx={cx} cy={cy} r={4} fill={color} stroke="var(--color-canvas)" strokeWidth={2} />
}

function linePath(points: [number, number][]): string {
  return points.map(([px, py], i) => `${i === 0 ? 'M' : 'L'}${px},${py}`).join('')
}

/** Round tick values from 0 to just above max, about four steps. */
export function niceTicks(max: number): number[] {
  const rough = max / 4
  const magnitude = 10 ** Math.floor(Math.log10(rough))
  const step = [1, 2, 2.5, 5, 10].map((m) => m * magnitude).find((s) => s >= rough) ?? 10 * magnitude
  const ticks: number[] = []
  for (let tick = 0; tick < max + step; tick += step) {
    ticks.push(Math.round(tick * 100) / 100)
  }
  return ticks
}

/** Pushes end labels apart vertically so they never overlap. */
function placeEndLabels<T extends { y: number }>(labels: T[]): T[] {
  const sorted = [...labels].sort((a, b) => a.y - b.y)
  for (let i = 1; i < sorted.length; i++) {
    const previous = sorted[i - 1]!
    const current = sorted[i]!
    if (current.y - previous.y < LABEL_MIN_GAP) {
      const middle = (current.y + previous.y) / 2
      sorted[i - 1] = { ...previous, y: middle - LABEL_MIN_GAP / 2 }
      sorted[i] = { ...current, y: middle + LABEL_MIN_GAP / 2 }
    }
  }
  return sorted
}

function useElementWidth<T extends HTMLElement>(fallback: number) {
  const ref = useRef<T>(null)
  const [width, setWidth] = useState(fallback)
  useLayoutEffect(() => {
    const element = ref.current
    if (!element || typeof ResizeObserver === 'undefined') {
      return
    }
    const observer = new ResizeObserver(([entry]) => {
      if (entry) {
        setWidth(Math.floor(entry.contentRect.width))
      }
    })
    observer.observe(element)
    return () => observer.disconnect()
  }, [])
  return [ref, width] as const
}
