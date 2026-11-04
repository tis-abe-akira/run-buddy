import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { Lap, RunDetail } from '../api/client'
import { RunDetailPanel } from './RunDetailPanel'

const lap = (number: number, startOffsetSeconds: number): Lap => ({
  number,
  startOffsetSeconds,
  distanceKm: 1,
  durationSeconds: 330,
  paceSecondsPerKm: 330,
  averageHeartRate: null,
  maxHeartRate: null,
  averageCadence: null,
  maxCadence: null,
  elevationGainMeters: null,
})

const detail = (route: RunDetail['route']): RunDetail => ({
  available: true,
  trackPoints: route.length,
  laps: [lap(1, 0), lap(2, 330)],
  route,
})

describe('RunDetailPanel', () => {
  it('highlights on the map the lap picked in the table', async () => {
    const user = userEvent.setup()
    const { container } = render(
      <RunDetailPanel
        detail={detail([
          { latitude: 0.0001, longitude: 0.0001, lap: 1 },
          { latitude: 0.0005, longitude: 0.0002, lap: 2 },
          { latitude: 0.0009, longitude: 0.0003, lap: 2 },
        ])}
      />,
    )

    await user.click(screen.getByRole('button', { name: 'ラップ 2 を地図で表示' }))
    expect(container.querySelectorAll('.route-highlight')).toHaveLength(1)

    await user.click(screen.getByRole('button', { name: 'ラップ 2 を地図で表示' }))
    expect(container.querySelector('.route-highlight')).toBeNull()
  })

  it('shows the laps without picking when the run has no positions', () => {
    render(<RunDetailPanel detail={detail([])} />)

    expect(screen.getByText('位置情報なし')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /地図で表示/ })).not.toBeInTheDocument()
  })
})
