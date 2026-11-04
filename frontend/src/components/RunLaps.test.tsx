import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { Lap } from '../api/client'
import { RunLaps } from './RunLaps'

const lap = (overrides: Partial<Lap> = {}): Lap => ({
  number: 1,
  startOffsetSeconds: 0,
  distanceKm: 1.2,
  durationSeconds: 396,
  paceSecondsPerKm: 330,
  averageHeartRate: 150,
  maxHeartRate: 162,
  averageCadence: 171,
  maxCadence: 184,
  elevationGainMeters: 8,
  ...overrides,
})

describe('RunLaps', () => {
  it('lists each lap with its pace, heart rate, cadence and climb', () => {
    render(
      <RunLaps
        state={{
          status: 'ready',
          detail: {
            available: true,
            trackPoints: 1833,
            route: [],
            laps: [lap(), lap({ number: 2, startOffsetSeconds: 330, distanceKm: 0.62, durationSeconds: 200, paceSecondsPerKm: 323, averageHeartRate: null, maxHeartRate: null })],
          },
        }}
      />,
    )

    const section = screen.getByRole('region', { name: 'ラップ' })
    const [, first, second] = within(section).getAllByRole('row')
    expect(within(first!).getByText('6:36')).toBeInTheDocument()
    expect(within(first!).getByText('5:30')).toBeInTheDocument()
    expect(within(first!).getByText('150 / 162')).toBeInTheDocument()
    expect(within(first!).getByText('171')).toBeInTheDocument()
    expect(within(first!).getByText('1.20')).toBeInTheDocument()
    expect(within(second!).getByText('0.62')).toBeInTheDocument()
    expect(within(second!).getByText('3:20')).toBeInTheDocument()
    expect(within(second!).getByText('—')).toBeInTheDocument()
  })

  it('picks a lap to show on the map, and lets go of it when picked again', async () => {
    const user = userEvent.setup()
    const onSelectLap = vi.fn()
    const detail = { available: true, trackPoints: 2, route: [], laps: [lap(), lap({ number: 2 })] }
    const { rerender } = render(
      <RunLaps state={{ status: 'ready', detail }} selectedLap={null} onSelectLap={onSelectLap} />,
    )

    await user.click(screen.getByRole('button', { name: 'ラップ 2 を地図で表示' }))
    expect(onSelectLap).toHaveBeenLastCalledWith(2)

    rerender(<RunLaps state={{ status: 'ready', detail }} selectedLap={2} onSelectLap={onSelectLap} />)
    const button = screen.getByRole('button', { name: 'ラップ 2 を地図で表示' })
    expect(button).toHaveAttribute('aria-pressed', 'true')
    expect(button.closest('tr')).toHaveClass('is-selected')

    await user.click(button)
    expect(onSelectLap).toHaveBeenLastCalledWith(null)
  })

  it('also picks a lap when its row is clicked anywhere', async () => {
    const user = userEvent.setup()
    const onSelectLap = vi.fn()
    render(
      <RunLaps
        state={{
          status: 'ready',
          detail: { available: true, trackPoints: 2, route: [], laps: [lap()] },
        }}
        selectedLap={null}
        onSelectLap={onSelectLap}
      />,
    )

    await user.click(screen.getByText('1.20'))
    expect(onSelectLap).toHaveBeenCalledWith(1)
  })

  it('offers no lap picking when there is no map to show it on', () => {
    render(
      <RunLaps
        state={{
          status: 'ready',
          detail: { available: true, trackPoints: 0, route: [], laps: [lap()] },
        }}
      />,
    )

    expect(screen.queryByRole('button', { name: /地図で表示/ })).not.toBeInTheDocument()
  })

  it('says a run has no laps when the file has none', () => {
    render(
      <RunLaps
        state={{
          status: 'ready',
          detail: { available: true, trackPoints: 0, route: [], laps: [] },
        }}
      />,
    )

    expect(screen.getByText('ラップの記録はありません')).toBeInTheDocument()
  })

  it('shows that it is loading', () => {
    render(<RunLaps state={{ status: 'loading' }} />)

    expect(screen.getByRole('status')).toHaveTextContent('ラップを読み込み中…')
  })

  it('says when the run has no detail data, as for a generated run', () => {
    render(
      <RunLaps
        state={{
          status: 'ready',
          detail: { available: false, trackPoints: 0, route: [], laps: [] },
        }}
      />,
    )

    expect(screen.getByText('このランには詳細データがありません')).toBeInTheDocument()
  })

  it('explains a failure and retries on request', async () => {
    const user = userEvent.setup()
    const onRetry = vi.fn()
    render(
      <RunLaps
        state={{
          status: 'error',
          message: 'Garmin の利用上限に達しました。',
          onRetry,
        }}
      />,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('Garmin の利用上限に達しました。')
    await user.click(screen.getByRole('button', { name: '再試行' }))
    expect(onRetry).toHaveBeenCalledOnce()
  })
})
