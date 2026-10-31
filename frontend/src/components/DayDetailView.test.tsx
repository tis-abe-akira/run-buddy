import { render, screen, within } from '@testing-library/react'
import { aRun } from '../test/runs'
import { DayDetailView } from './DayDetailView'

describe('DayDetailView', () => {
  it('shows what was planned for the day', () => {
    render(
      <DayDetailView
        day={{ date: '2026-11-08', plannedWorkouts: [{ date: '2026-11-08', type: 'LONG', distanceKm: 18, note: 'ゆっくり' }], runs: [] }}
      />,
    )

    const plan = screen.getByRole('region', { name: '予定' })
    expect(within(plan).getByText('ロング走')).toBeInTheDocument()
    expect(within(plan).getByText('18.0 km')).toBeInTheDocument()
    expect(within(plan).getByText('ゆっくり')).toBeInTheDocument()
  })

  it('says so when nothing was planned or run', () => {
    render(<DayDetailView day={{ date: '2026-11-09', plannedWorkouts: [], runs: [] }} />)

    expect(screen.getByText('予定はありません')).toBeInTheDocument()
    expect(screen.getByText('この日のランはありません')).toBeInTheDocument()
  })

  it('shows every measurement of a run', () => {
    render(
      <DayDetailView
        day={{
          date: '2026-11-08',
          plannedWorkouts: [],
          runs: [
            aRun({
              startedAt: '2026-11-08T06:12:41',
              distanceKm: 16.13,
              durationSeconds: 6049,
              paceSecondsPerKm: 375,
              averageHeartRate: 164,
              maxHeartRate: 181,
              averageCadence: 172,
              maxCadence: 188,
              averageStrideLengthCm: 98,
              elevationGainMeters: 42,
            }),
          ],
        }}
      />,
    )

    const run = screen.getByRole('region', { name: '6:12 スタート' })
    expect(within(run).getByText('16.1')).toBeInTheDocument()
    expect(within(run).getByText('1:40:49')).toBeInTheDocument()
    expect(within(run).getByText('6:15')).toBeInTheDocument()
    expect(measurement(run, '心拍 平均')).toHaveTextContent('164')
    expect(measurement(run, '心拍 最大')).toHaveTextContent('181')
    expect(measurement(run, 'ケイデンス 平均')).toHaveTextContent('172')
    expect(measurement(run, 'ケイデンス 最大')).toHaveTextContent('188')
    expect(measurement(run, '歩幅')).toHaveTextContent('98')
    expect(measurement(run, '上昇')).toHaveTextContent('42')
  })

  it('marks measurements the watch did not record', () => {
    render(<DayDetailView day={{ date: '2026-11-02', plannedWorkouts: [], runs: [aRun({ startedAt: '2026-11-02T06:30:00' })] }} />)

    const run = screen.getByRole('region', { name: '6:30 スタート' })
    expect(measurement(run, '心拍 最大')).toHaveTextContent('—')
    expect(measurement(run, '上昇')).toHaveTextContent('—')
  })

  it('numbers the runs of a day with more than one, in the order they started', () => {
    render(
      <DayDetailView
        day={{
          date: '2026-11-08',
          plannedWorkouts: [],
          runs: [aRun({ activityId: 'm', startedAt: '2026-11-08T06:12:00' }), aRun({ activityId: 'e', startedAt: '2026-11-08T18:40:00' })],
        }}
      />,
    )

    const runs = screen.getAllByRole('region', { name: /スタート$/ })
    expect(runs.map((r) => r.getAttribute('aria-label'))).toEqual(['ラン 1 · 6:12 スタート', 'ラン 2 · 18:40 スタート'])
  })
})

/** The value next to a measurement's label. */
function measurement(scope: HTMLElement, label: string): HTMLElement {
  return within(scope).getByText(label).nextElementSibling as HTMLElement
}
