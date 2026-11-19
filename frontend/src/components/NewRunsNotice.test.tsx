import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { NewRunsNotice } from './NewRunsNotice'

const one = [{ activityId: 'g-1', date: '2026-11-19' }]

describe('NewRunsNotice', () => {
  it('shows nothing while there is nothing new', () => {
    const { container } = render(<NewRunsNotice newRuns={[]} importing={false} onImport={vi.fn()} />)

    expect(container).toBeEmptyDOMElement()
  })

  it('offers to import a new run, naming its day', async () => {
    const user = userEvent.setup()
    const onImport = vi.fn()
    render(<NewRunsNotice newRuns={one} importing={false} onImport={onImport} />)

    await user.click(screen.getByRole('button', { name: '11/19 のランを取り込む' }))

    expect(onImport).toHaveBeenCalledOnce()
  })

  it('counts several new runs', () => {
    render(
      <NewRunsNotice
        newRuns={[...one, { activityId: 'g-0', date: '2026-11-18' }]}
        importing={false}
        onImport={vi.fn()}
      />,
    )

    expect(screen.getByRole('button', { name: '新しいラン 2 件を取り込む' })).toBeInTheDocument()
  })

  it('cannot be pressed twice while importing', () => {
    render(<NewRunsNotice newRuns={one} importing onImport={vi.fn()} />)

    expect(screen.getByRole('button', { name: '取り込み中…' })).toBeDisabled()
  })

  it('says why the import failed and lets the runner try again', () => {
    render(<NewRunsNotice newRuns={one} importing={false} error="Garmin の利用上限に達しました。" onImport={vi.fn()} />)

    expect(screen.getByRole('alert')).toHaveTextContent('Garmin の利用上限に達しました。')
    expect(screen.getByRole('button', { name: '11/19 のランを取り込む' })).toBeEnabled()
  })
})
