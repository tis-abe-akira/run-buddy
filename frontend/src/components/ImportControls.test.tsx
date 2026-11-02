import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ImportControls } from './ImportControls'

const idle = { onImportLatest: vi.fn(), onImportMonth: vi.fn(), importing: null, month: '2026-11' } as const

describe('ImportControls', () => {
  it('makes importing the latest runs the main action, and re-reading the month the second one', async () => {
    const user = userEvent.setup()
    const onImportLatest = vi.fn()
    const onImportMonth = vi.fn()
    render(<ImportControls {...idle} onImportLatest={onImportLatest} onImportMonth={onImportMonth} />)

    await user.click(screen.getByRole('button', { name: '最新を取り込む' }))
    await user.click(screen.getByRole('button', { name: '11月を取り込み直す' }))

    expect(onImportLatest).toHaveBeenCalledOnce()
    expect(onImportMonth).toHaveBeenCalledOnce()
  })

  it('says which days the latest import looked at and what it found', () => {
    render(<ImportControls {...idle} lastLatest={{ from: '2026-10-31', to: '2026-11-02', fetched: 2, added: 1 }} />)

    expect(screen.getByRole('status')).toHaveTextContent('10/31〜11/2 を確認し、新しく 1 件を取り込みました')
  })

  it('says so when nothing was new', () => {
    render(<ImportControls {...idle} lastLatest={{ from: '2026-11-01', to: '2026-11-02', fetched: 1, added: 0 }} />)

    expect(screen.getByRole('status')).toHaveTextContent('11/1〜11/2 に新しいランはありませんでした')
  })

  it('reports a month re-read as before', () => {
    render(<ImportControls {...idle} lastMonth={{ month: '2026-11', fetched: 24, added: 0 }} />)

    expect(screen.getByRole('status')).toHaveTextContent('11月の 24 件を確認し、新しく 0 件を取り込みました')
  })

  it('shows which import is running and blocks both while it does', () => {
    render(<ImportControls {...idle} importing="latest" />)

    expect(screen.getByRole('button', { name: '取り込み中…' })).toBeDisabled()
    expect(screen.getByRole('button', { name: '11月を取り込み直す' })).toBeDisabled()
  })

  it('shows what went wrong', () => {
    render(<ImportControls {...idle} error="Garmin の利用上限に達しました。" />)

    expect(screen.getByRole('alert')).toHaveTextContent('Garmin の利用上限に達しました。')
  })
})
