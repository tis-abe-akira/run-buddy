import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { Plan } from '../api/client'
import { PlanEditor } from './PlanEditor'

const EMPTY_PLAN: Plan = { targetKm: 0, workouts: [] }

function renderEditor(onSave = vi.fn(), initial: Plan = EMPTY_PLAN) {
  render(<PlanEditor month="2026-11" initial={initial} onSave={onSave} saving={false} saved={false} />)
  return onSave
}

describe('PlanEditor', () => {
  it('sends the target and workouts as numbers', async () => {
    const user = userEvent.setup()
    const onSave = renderEditor()

    await user.type(screen.getByLabelText('月間目標 km'), '150')
    await user.click(screen.getByRole('button', { name: '＋ ワークアウトを追加' }))
    await user.clear(screen.getByLabelText('1件目の日付'))
    await user.type(screen.getByLabelText('1件目の日付'), '2026-11-08')
    await user.selectOptions(screen.getByLabelText('1件目の種別'), 'LONG')
    await user.type(screen.getByLabelText('1件目の距離'), '20')
    await user.type(screen.getByLabelText('1件目のメモ'), 'LSD')
    await user.click(screen.getByRole('button', { name: 'プランを保存' }))

    expect(onSave).toHaveBeenCalledWith({
      targetKm: 150,
      workouts: [{ date: '2026-11-08', type: 'LONG', distanceKm: 20, note: 'LSD' }],
    })
  })

  it('shows the planned total against the target while editing', async () => {
    const user = userEvent.setup()
    renderEditor(vi.fn(), {
      targetKm: 100,
      workouts: [
        { date: '2026-11-02', type: 'EASY', distanceKm: 10, note: '' },
        { date: '2026-11-08', type: 'LONG', distanceKm: 20, note: '' },
      ],
    })

    expect(screen.getByText('30.0')).toBeInTheDocument()
    expect(screen.getByText(/目標の 30%/)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '1件目を削除' }))

    expect(screen.getByText('20.0')).toBeInTheDocument()
  })

  it('does not save a workout without a distance', async () => {
    const user = userEvent.setup()
    const onSave = renderEditor()

    await user.click(screen.getByRole('button', { name: '＋ ワークアウトを追加' }))
    await user.click(screen.getByRole('button', { name: 'プランを保存' }))

    expect(screen.getByText('距離は0より大きい数値にしてください')).toBeInTheDocument()
    expect(onSave).not.toHaveBeenCalled()
  })

  it('does not save a workout dated outside the month', async () => {
    const user = userEvent.setup()
    const onSave = renderEditor(vi.fn(), {
      targetKm: 0,
      workouts: [{ date: '2026-12-01', type: 'EASY', distanceKm: 5, note: '' }],
    })

    await user.click(screen.getByRole('button', { name: 'プランを保存' }))

    expect(screen.getByText('2026年11月の日付を選んでください')).toBeInTheDocument()
    expect(onSave).not.toHaveBeenCalled()
  })

  it('shows the error returned by the server', () => {
    render(
      <PlanEditor month="2026-11" initial={EMPTY_PLAN} onSave={vi.fn()} saving={false} saved={false} serverError="boom" />,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('boom')
  })
})
