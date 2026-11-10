import { useQuery } from '@tanstack/react-query'
import { createFileRoute, Link, notFound } from '@tanstack/react-router'
import { dayQuery, runDetailQuery, runReviewQuery, useReviewRun } from '../api/queries'
import { DayDetailView } from '../components/DayDetailView'
import { ErrorBand } from '../components/ErrorBand'
import { RunDetailPanel } from '../components/RunDetailPanel'
import { RunLaps } from '../components/RunLaps'
import { RunReviewPanel } from '../components/RunReviewPanel'
import { formatDayHeading, isIsoDate, monthOfDate, shiftDay } from '../lib/day'
import { describeDetailError } from '../lib/importError'
import { describeReviewError } from '../lib/reviewError'

export const Route = createFileRoute('/days/$date')({
  beforeLoad: ({ params }) => {
    if (!isIsoDate(params.date)) {
      throw notFound()
    }
  },
  loader: ({ context, params }) => context.queryClient.ensureQueryData(dayQuery(params.date)),
  component: DayPage,
  errorComponent: ({ error }) => <ErrorBand error={error} />,
})

function DayPage() {
  const { date } = Route.useParams()
  const day = useQuery(dayQuery(date))

  if (!day.data) {
    return null
  }
  return (
    <>
      <section className="band">
        <div className="container row" style={{ alignItems: 'flex-end', gap: 'var(--spacing-lg)' }}>
          <div className="stack" style={{ gap: 'var(--spacing-xs)' }}>
            <span className="caption-uppercase muted">Day review</span>
            <h1 className="display-lg">{formatDayHeading(date)}</h1>
          </div>
          <span className="spacer" />
          <div className="row">
            <Link to="/days/$date" params={{ date: shiftDay(date, -1) }} className="button-icon" aria-label="前の日">
              ‹
            </Link>
            <Link to="/days/$date" params={{ date: shiftDay(date, 1) }} className="button-icon" aria-label="次の日">
              ›
            </Link>
            <Link
              to="/dashboard"
              search={{ month: monthOfDate(date) }}
              className="text-link body-sm"
              style={{ marginLeft: 'var(--spacing-xs)' }}
            >
              この月のダッシュボードへ
            </Link>
          </div>
        </div>
      </section>
      <section className="band">
        <div className="container">
          <DayDetailView day={day.data} renderDetail={(run) => (
              <>
                <RunReviewLoader activityId={run.activityId} />
                <RunDetailLoader activityId={run.activityId} />
              </>
            )} />
        </div>
      </section>
    </>
  )
}

/** Loads one run's course and laps; the first look downloads its activity file, so they load apart from the page. */
function RunDetailLoader({ activityId }: { activityId: string }) {
  const detail = useQuery(runDetailQuery(activityId))
  if (detail.isPending) {
    return <RunLaps state={{ status: 'loading' }} />
  }
  if (detail.isError) {
    return <RunLaps state={{ status: 'error', message: describeDetailError(detail.error), onRetry: () => void detail.refetch() }} />
  }
  return <RunDetailPanel detail={detail.data} />
}

/** Loads a run's saved AI review and asks for a new one on request (#35); apart from the course and laps. */
function RunReviewLoader({ activityId }: { activityId: string }) {
  const saved = useQuery(runReviewQuery(activityId))
  const reviewRun = useReviewRun(activityId)
  const state = saved.isPending
    ? ({ status: 'loading' } as const)
    : saved.isError
      ? ({ status: 'error', message: describeReviewError(saved.error) } as const)
      : ({ status: 'ready', review: saved.data.review } as const)
  return (
    <RunReviewPanel
      state={state}
      reviewing={reviewRun.isPending}
      reviewError={reviewRun.error ? describeReviewError(reviewRun.error) : undefined}
      onReview={() => reviewRun.mutate()}
    />
  )
}
