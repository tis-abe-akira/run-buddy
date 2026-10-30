import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { RunsTable } from './RunsTable'

describe('RunsTable', () => {
  it('lists the latest run first', () => {
    render(
      <RunsTable
        runs={[
          { activityId: 'a', startedAt: '2026-11-02T06:30:00', distanceKm: 8, durationSeconds: 2640, paceSecondsPerKm: 330, averageHeartRate: 150 },
          { activityId: 'b', startedAt: '2026-11-08T06:30:00', distanceKm: 20, durationSeconds: 7200, paceSecondsPerKm: 360, averageHeartRate: null },
        ]}
      />,
    )

    const [, first, second] = screen.getAllByRole('row')
    expect(within(first!).getByText('11/8（日）')).toBeInTheDocument()
    expect(within(first!).getByText('2:00:00')).toBeInTheDocument()
    expect(within(first!).getByText('—')).toBeInTheDocument()
    expect(within(second!).getByText('5:30')).toBeInTheDocument()
  })

  it('explains what to do when there are no runs', () => {
    render(<RunsTable runs={[]} />)

    expect(screen.getByText(/Garminから取り込む/)).toBeInTheDocument()
  })
})

describe('RunsTable with many runs', () => {
  it('shows the latest seven until expanded', async () => {
    const user = userEvent.setup()
    const runs = Array.from({ length: 10 }, (_, i) => ({
      activityId: `r${i}`,
      startedAt: `2026-11-${String(i + 1).padStart(2, '0')}T06:30:00`,
      distanceKm: 5,
      durationSeconds: 1800,
      paceSecondsPerKm: 360,
      averageHeartRate: 140,
    }))
    render(<RunsTable runs={runs} />)

    expect(screen.getAllByRole('row')).toHaveLength(1 + 7)

    await user.click(screen.getByRole('button', { name: 'すべて表示（10 件）' }))

    expect(screen.getAllByRole('row')).toHaveLength(1 + 10)
  })
})
