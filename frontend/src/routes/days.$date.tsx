import { useQuery } from '@tanstack/react-query'
import { createFileRoute, Link, notFound } from '@tanstack/react-router'
import { dayQuery, runDetailQuery } from '../api/queries'
import { DayDetailView } from '../components/DayDetailView'
import { ErrorBand } from '../components/ErrorBand'
import { RunLaps } from '../components/RunLaps'
import { formatDayHeading, isIsoDate, monthOfDate, shiftDay } from '../lib/day'
import { describeDetailError } from '../lib/importError'

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
          <DayDetailView day={day.data} renderLaps={(run) => <RunLapsLoader activityId={run.activityId} />} />
        </div>
      </section>
    </>
  )
}

/** Loads one run's laps; the first look downloads its activity file, so it loads apart from the page. */
function RunLapsLoader({ activityId }: { activityId: string }) {
  const detail = useQuery(runDetailQuery(activityId))
  if (detail.isPending) {
    return <RunLaps state={{ status: 'loading' }} />
  }
  if (detail.isError) {
    return <RunLaps state={{ status: 'error', message: describeDetailError(detail.error), onRetry: () => void detail.refetch() }} />
  }
  return <RunLaps state={{ status: 'ready', detail: detail.data }} />
}
