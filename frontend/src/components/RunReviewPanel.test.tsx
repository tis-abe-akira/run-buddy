import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { RunReview } from '../api/client'
import { RunReviewPanel } from './RunReviewPanel'

const review: RunReview = {
  headline: '安定した10km',
  runItself: '前半から一定のペースで走れています。',
  comparison: '先週の似たランより心拍が5低い。',
  reviewer: 'gemini',
  model: 'gemini-2.5-flash',
  reviewedAt: '2026-11-02T09:00:00Z',
}

describe('RunReviewPanel', () => {
  it('offers a review when the run has none yet', async () => {
    const user = userEvent.setup()
    const onReview = vi.fn()
    render(<RunReviewPanel state={{ status: 'ready', review: null }} reviewing={false} onReview={onReview} />)

    expect(screen.getByRole('region', { name: 'AI の評価' })).toHaveTextContent('まだ評価していません')
    await user.click(screen.getByRole('button', { name: 'AI に評価してもらう' }))
    expect(onReview).toHaveBeenCalledOnce()
  })

  it('shows the saved review in its three parts, with who wrote it, and offers to review again', async () => {
    const user = userEvent.setup()
    const onReview = vi.fn()
    render(<RunReviewPanel state={{ status: 'ready', review }} reviewing={false} onReview={onReview} />)

    expect(screen.getByRole('heading', { name: '安定した10km' })).toBeInTheDocument()
    expect(screen.getByText('前半から一定のペースで走れています。')).toBeInTheDocument()
    expect(screen.getByText('先週の似たランより心拍が5低い。')).toBeInTheDocument()
    expect(screen.getByText(/gemini-2\.5-flash/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '評価し直す' }))
    expect(onReview).toHaveBeenCalledOnce()
  })

  it('says it is reviewing and cannot be pressed twice', () => {
    render(<RunReviewPanel state={{ status: 'ready', review: null }} reviewing onReview={vi.fn()} />)

    expect(screen.getByRole('status')).toHaveTextContent('AI が評価しています')
    expect(screen.getByRole('button', { name: 'AI に評価してもらう' })).toBeDisabled()
  })

  it('counts the seconds while the AI is reviewing, and starts again from zero next time', () => {
    vi.useFakeTimers()
    try {
      const props = { state: { status: 'ready', review: null } as const, onReview: vi.fn() }
      const { rerender } = render(<RunReviewPanel {...props} reviewing />)

      act(() => vi.advanceTimersByTime(12_000))
      expect(screen.getByRole('status')).toHaveTextContent('AI が評価しています…（12 秒）')

      rerender(<RunReviewPanel {...props} reviewing={false} />)
      rerender(<RunReviewPanel {...props} reviewing />)
      act(() => vi.advanceTimersByTime(3_000))
      expect(screen.getByRole('status')).toHaveTextContent('（3 秒）')
    } finally {
      vi.useRealTimers()
    }
  })

  it('holds the place of the first review while it is being written', () => {
    const { container } = render(<RunReviewPanel state={{ status: 'ready', review: null }} reviewing onReview={vi.fn()} />)

    expect(container.querySelector('.review-skeleton')).not.toBeNull()
    expect(screen.queryByText('このランはまだ評価していません。')).not.toBeInTheDocument()
  })

  it('marks the old review as being replaced while it is reviewed again', () => {
    render(<RunReviewPanel state={{ status: 'ready', review }} reviewing onReview={vi.fn()} />)

    const old = screen.getByRole('heading', { name: '安定した10km' }).closest('.review-body')
    expect(old).toHaveClass('review-stale')
    expect(old).toHaveAttribute('aria-busy', 'true')
    expect(screen.getByText('評価し直しています。終わるまで前の評価を表示しています。')).toBeInTheDocument()
  })

  it('explains a failed review and keeps the button to try again', () => {
    render(
      <RunReviewPanel
        state={{ status: 'ready', review }}
        reviewing={false}
        reviewError="AI の利用上限に達しました。"
        onReview={vi.fn()}
      />,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('AI の利用上限に達しました。')
    expect(screen.getByRole('heading', { name: '安定した10km' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '評価し直す' })).toBeEnabled()
  })

  it('shows that the saved review is loading, and why it could not be loaded', () => {
    const { rerender } = render(<RunReviewPanel state={{ status: 'loading' }} reviewing={false} onReview={vi.fn()} />)
    expect(screen.getByRole('status')).toHaveTextContent('読み込み中')

    rerender(<RunReviewPanel state={{ status: 'error', message: '読み込めませんでした' }} reviewing={false} onReview={vi.fn()} />)
    expect(screen.getByRole('alert')).toHaveTextContent('読み込めませんでした')
  })
})
