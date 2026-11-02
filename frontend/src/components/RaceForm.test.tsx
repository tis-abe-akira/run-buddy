import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { aRace } from '../test/races'
import { RaceForm } from './RaceForm'

describe('RaceForm', () => {
  it('sends a full marathon with a target time', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()
    render(<RaceForm onSubmit={onSubmit} saving={false} />)

    await user.type(screen.getByLabelText('大会名'), 'Autumn marathon')
    await user.type(screen.getByLabelText('日付'), '2026-11-22')
    await user.selectOptions(screen.getByLabelText('距離'), 'フルマラソン')
    await user.type(screen.getByLabelText('目標タイム（任意）'), '5:00:00')
    await user.click(screen.getByRole('button', { name: '登録する' }))

    expect(onSubmit).toHaveBeenCalledWith({ name: 'Autumn marathon', date: '2026-11-22', distanceKm: 42.195, targetSeconds: 18000 })
  })

  it('without a target time the goal is to finish', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()
    render(<RaceForm onSubmit={onSubmit} saving={false} />)

    await user.type(screen.getByLabelText('大会名'), 'Spring half')
    await user.type(screen.getByLabelText('日付'), '2027-03-14')
    await user.selectOptions(screen.getByLabelText('距離'), 'ハーフマラソン')
    await user.click(screen.getByRole('button', { name: '登録する' }))

    expect(onSubmit).toHaveBeenCalledWith({ name: 'Spring half', date: '2027-03-14', distanceKm: 21.0975, targetSeconds: null })
  })

  it('takes any other distance in kilometers', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()
    render(<RaceForm onSubmit={onSubmit} saving={false} />)

    await user.type(screen.getByLabelText('大会名'), 'Trail 30k')
    await user.type(screen.getByLabelText('日付'), '2027-05-01')
    await user.selectOptions(screen.getByLabelText('距離'), 'その他')
    await user.type(screen.getByLabelText('距離（km）'), '30')
    await user.click(screen.getByRole('button', { name: '登録する' }))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ distanceKm: 30 }))
  })

  it('explains what is wrong instead of sending', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()
    render(<RaceForm onSubmit={onSubmit} saving={false} />)

    await user.type(screen.getByLabelText('目標タイム（任意）'), '5時間')
    await user.click(screen.getByRole('button', { name: '登録する' }))

    expect(onSubmit).not.toHaveBeenCalled()
    expect(screen.getByText('大会名を入力してください')).toBeInTheDocument()
    expect(screen.getByText('日付を入力してください')).toBeInTheDocument()
    expect(screen.getByText('目標タイムは 5:00:00 や 4:30 の形で入力してください')).toBeInTheDocument()
  })

  it('starts from the race being edited', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()
    render(<RaceForm initial={aRace()} onSubmit={onSubmit} saving={false} />)

    expect(screen.getByLabelText('大会名')).toHaveValue('Autumn marathon')
    expect(screen.getByLabelText('目標タイム（任意）')).toHaveValue('5:00:00')
    await user.click(screen.getByRole('button', { name: '保存する' }))

    expect(onSubmit).toHaveBeenCalledWith({ name: 'Autumn marathon', date: '2026-11-22', distanceKm: 42.195, targetSeconds: 18000 })
  })
})
