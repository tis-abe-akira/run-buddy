import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { aRun } from '../test/runs'
import { RunsTable } from './RunsTable'

describe('RunsTable', () => {
  it('lists the latest run first', () => {
    render(
      <RunsTable
        runs={[
          aRun({ activityId: 'a', startedAt: '2026-11-02T06:30:00', distanceKm: 8, durationSeconds: 2640, paceSecondsPerKm: 330, averageHeartRate: 150 }),
          aRun({ activityId: 'b', startedAt: '2026-11-08T06:30:00', distanceKm: 20, durationSeconds: 7200, paceSecondsPerKm: 360, averageHeartRate: null }),
        ]}
      />,
    )

    const [, first, second] = screen.getAllByRole('row')
    expect(first).toHaveTextContent('11/8（日）')
    expect(within(first!).getByText('2:00:00')).toBeInTheDocument()
    expect(within(first!).getByText('—')).toBeInTheDocument()
    expect(within(second!).getByText('5:30')).toBeInTheDocument()
  })

  it('marks Saturday and Sunday runs', () => {
    render(
      <RunsTable
        runs={[
          aRun({ activityId: 'sat', startedAt: '2026-11-07T06:30:00', distanceKm: 20, durationSeconds: 7200, paceSecondsPerKm: 360, averageHeartRate: 140 }),
          aRun({ activityId: 'mon', startedAt: '2026-11-09T06:30:00', distanceKm: 5, durationSeconds: 1800, paceSecondsPerKm: 360, averageHeartRate: 140 }),
        ]}
      />,
    )

    const [, monday, saturday] = screen.getAllByRole('row')
    expect(saturday).toHaveClass('weekend-row')
    expect(within(saturday!).getByText('（土）')).toBeInTheDocument()
    expect(monday).not.toHaveClass('weekend-row')
  })

  it('links each date to its day through the given link', () => {
    render(
      <RunsTable
        runs={[aRun({ startedAt: '2026-11-08T06:30:00' })]}
        dayLink={(date, label) => <a href={`/days/${date}`}>{label}</a>}
      />,
    )

    expect(screen.getByRole('link', { name: '11/8（日）' })).toHaveAttribute('href', '/days/2026-11-08')
  })

  it('explains what to do when there are no runs', () => {
    render(<RunsTable runs={[]} />)

    expect(screen.getByText(/Garminから取り込む/)).toBeInTheDocument()
  })
})

describe('RunsTable with many runs', () => {
  it('shows the latest seven until expanded', async () => {
    const user = userEvent.setup()
    const runs = Array.from({ length: 10 }, (_, i) =>
      aRun({ activityId: `r${i}`, startedAt: `2026-11-${String(i + 1).padStart(2, '0')}T06:30:00` }),
    )
    render(<RunsTable runs={runs} />)

    expect(screen.getAllByRole('row')).toHaveLength(1 + 7)

    await user.click(screen.getByRole('button', { name: 'すべて表示（10 件）' }))

    expect(screen.getAllByRole('row')).toHaveLength(1 + 10)
  })
})
