import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { aRace } from '../test/races'
import { RacesView } from './RacesView'

const noop = () => {}

describe('RacesView', () => {
  it('shows upcoming races with the days left and the goal, and past ones apart', () => {
    render(
      <RacesView
        races={[
          aRace({ id: 'past', name: 'October 10k', date: '2026-10-04', distanceKm: 10, targetSeconds: null, targetPaceSecondsPerKm: null, daysUntil: -28 }),
          aRace(),
        ]}
        onCreate={noop}
        onUpdate={noop}
        onDelete={noop}
        saving={false}
      />,
    )

    const upcoming = screen.getByRole('region', { name: 'これからのレース' })
    expect(within(upcoming).getByText('Autumn marathon')).toBeInTheDocument()
    expect(within(upcoming).getByText('あと 21 日')).toBeInTheDocument()
    expect(within(upcoming).getByText('目標 5:00:00 · 7:06/km')).toBeInTheDocument()
    const past = screen.getByRole('region', { name: '終わったレース' })
    expect(within(past).getByText('October 10k')).toBeInTheDocument()
    expect(within(past).getByText('完走')).toBeInTheDocument()
  })

  it('invites the first race when there is none', () => {
    render(<RacesView races={[]} onCreate={noop} onUpdate={noop} onDelete={noop} saving={false} />)

    expect(screen.getByText(/目標のレースを登録すると/)).toBeInTheDocument()
  })

  it('asks once more before deleting', async () => {
    const user = userEvent.setup()
    const onDelete = vi.fn()
    render(<RacesView races={[aRace()]} onCreate={noop} onUpdate={noop} onDelete={onDelete} saving={false} />)

    await user.click(screen.getByRole('button', { name: 'Autumn marathon を削除' }))
    expect(onDelete).not.toHaveBeenCalled()
    await user.click(screen.getByRole('button', { name: '削除する' }))

    expect(onDelete).toHaveBeenCalledWith('race-1')
  })

  it('edits a race in place', async () => {
    const user = userEvent.setup()
    const onUpdate = vi.fn()
    render(<RacesView races={[aRace()]} onCreate={noop} onUpdate={onUpdate} onDelete={noop} saving={false} />)

    await user.click(screen.getByRole('button', { name: 'Autumn marathon を編集' }))
    // The form for a new race has the same labels, so work inside the one being edited.
    const editing = within(screen.getByRole('button', { name: '保存する' }).closest('form')!)
    await user.clear(editing.getByLabelText('目標タイム（任意）'))
    await user.type(editing.getByLabelText('目標タイム（任意）'), '4:45:00')
    await user.click(editing.getByRole('button', { name: '保存する' }))

    expect(onUpdate).toHaveBeenCalledWith('race-1', expect.objectContaining({ targetSeconds: 17100 }))
  })
})
