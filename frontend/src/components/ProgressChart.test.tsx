import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { DashboardDay } from '../api/client'
import { niceTicks, ProgressChart } from './ProgressChart'

/** Ten days: 5 km planned on odd days, 6 km run on even days. */
function tenDays(): DashboardDay[] {
  let planned = 0
  let actual = 0
  return Array.from({ length: 10 }, (_, i) => {
    const plannedKm = i % 2 === 0 ? 5 : 0
    const actualKm = i % 2 === 1 ? 6 : 0
    planned += plannedKm
    actual += actualKm
    return {
      date: `2026-11-${String(i + 1).padStart(2, '0')}`,
      plannedKm,
      actualKm,
      cumulativePlannedKm: planned,
      cumulativeActualKm: actual,
    }
  })
}

describe('ProgressChart', () => {
  it('labels the end of each series and the target', () => {
    render(<ProgressChart days={tenDays()} targetKm={40} elapsedDays={10} />)

    expect(screen.getByText('実績 30.0')).toBeInTheDocument()
    expect(screen.getByText('計画 25.0')).toBeInTheDocument()
    expect(screen.getByText('目標 40.0 km')).toBeInTheDocument()
  })

  it('stops the actual series at the last elapsed day', () => {
    render(<ProgressChart days={tenDays()} targetKm={0} elapsedDays={4} />)

    expect(screen.getByText('実績 12.0')).toBeInTheDocument()
  })

  it('has no actual series for a month that has not started', () => {
    render(<ProgressChart days={tenDays()} targetKm={0} elapsedDays={0} />)

    expect(screen.queryByText(/^実績 /)).not.toBeInTheDocument()
  })

  it('shades Saturdays and Sundays behind the lines', () => {
    const { container } = render(<ProgressChart days={tenDays()} targetKm={40} elapsedDays={10} />)

    // 2026-11-01 is a Sunday, 11-07 a Saturday and 11-08 a Sunday.
    const bands = [...container.querySelectorAll('rect[data-weekend]')].map((r) => r.getAttribute('data-weekend'))
    expect(bands).toEqual(['2026-11-01', '2026-11-07', '2026-11-08'])
  })

  it('offers the data as a table', async () => {
    const user = userEvent.setup()
    render(<ProgressChart days={tenDays()} targetKm={40} elapsedDays={10} />)

    await user.click(screen.getByRole('button', { name: '表で見る' }))

    // header + every day has either a plan or a run
    expect(screen.getAllByRole('row')).toHaveLength(11)
  })
})

describe('niceTicks', () => {
  it.each([
    [10, [0, 2.5, 5, 7.5, 10]],
    [150, [0, 50, 100, 150]],
    [213, [0, 100, 200, 300]],
  ])('covers %d with round steps', (max, expected) => {
    expect(niceTicks(max)).toEqual(expected)
  })
})
