import type { RunReview } from '../api/client'
import { formatDate } from '../lib/format'

export type SavedReviewState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'ready'; review: RunReview | null }

type Props = {
  /** The saved review, loaded on its own. */
  state: SavedReviewState
  /** A review is being written: the AI is called, which takes a few seconds. */
  reviewing: boolean
  /** Why the last request for a review failed; the saved review, if any, stays shown. */
  reviewError?: string
  /** Asks the AI for a (new) review. Nothing happens without this: reviews are written on request (#35). */
  onReview: () => void
}

/**
 * A run's AI review (#35, ADR-0016): a headline, the run itself and how it compares with similar recent
 * runs. Reviews are written only when asked, and kept.
 */
export function RunReviewPanel({ state, reviewing, reviewError, onReview }: Props) {
  return (
    <section className="stack run-review" aria-label="AI の評価" style={{ gap: 'var(--spacing-sm)' }}>
      <h3 className="caption-uppercase muted">AI の評価</h3>
      <Body state={state} reviewing={reviewing} reviewError={reviewError} onReview={onReview} />
    </section>
  )
}

function Body({ state, reviewing, reviewError, onReview }: Props) {
  if (state.status === 'loading') {
    return (
      <p className="body-sm muted" role="status" style={{ margin: 0 }}>
        評価を読み込み中…
      </p>
    )
  }
  if (state.status === 'error') {
    return (
      <p className="field-error" role="alert" style={{ margin: 0 }}>
        {state.message}
      </p>
    )
  }
  const review = state.review
  return (
    <>
      {review === null ? (
        <p className="body-sm muted" style={{ margin: 0 }}>
          このランはまだ評価していません。
        </p>
      ) : (
        <div className="stack" style={{ gap: 'var(--spacing-xs)' }}>
          <h4 className="title-md" style={{ margin: 0 }}>
            {review.headline}
          </h4>
          <p className="body-sm" style={{ margin: 0 }}>
            {review.runItself}
          </p>
          <p className="body-sm" style={{ margin: 0 }}>
            {review.comparison}
          </p>
          <span className="caption muted numeric">
            {review.reviewer} · {review.model} · {formatDate(review.reviewedAt)} の評価
          </span>
        </div>
      )}
      {reviewError && (
        <p className="field-error" role="alert" style={{ margin: 0 }}>
          {reviewError}
        </p>
      )}
      <div className="row">
        <button type="button" className="button button-secondary" disabled={reviewing} onClick={onReview}>
          {review === null ? 'AI に評価してもらう' : '評価し直す'}
        </button>
        {reviewing && (
          <span className="body-sm muted" role="status">
            AI が評価しています…
          </span>
        )}
      </div>
    </>
  )
}
