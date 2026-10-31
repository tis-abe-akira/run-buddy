import { useQuery } from '@tanstack/react-query'
import { createFileRoute } from '@tanstack/react-router'
import { activitySourceQuery, dashboardQuery, runsQuery, useImportRuns } from '../api/queries'
import { DashboardView } from '../components/DashboardView'
import { ErrorBand } from '../components/ErrorBand'
import { elapsedDays } from '../lib/elapsed'
import { describeImportError } from '../lib/importError'
import { currentMonth, isMonth, type Month } from '../lib/month'

type DashboardSearch = { month?: Month }

export const Route = createFileRoute('/dashboard')({
  // An invalid ?month= is dropped rather than rejected, so the page falls back to this month.
  validateSearch: (search: Record<string, unknown>): DashboardSearch =>
    isMonth(search.month) ? { month: search.month } : {},
  loaderDeps: ({ search }) => ({ month: search.month ?? currentMonth() }),
  loader: ({ context, deps }) =>
    Promise.all([
      context.queryClient.ensureQueryData(dashboardQuery(deps.month)),
      context.queryClient.ensureQueryData(runsQuery(deps.month)),
      context.queryClient.ensureQueryData(activitySourceQuery()),
    ]),
  component: DashboardPage,
  errorComponent: ({ error }) => <ErrorBand error={error} />,
})

function DashboardPage() {
  const { month: requested } = Route.useSearch()
  const month = requested ?? currentMonth()
  const navigate = Route.useNavigate()
  const dashboard = useQuery(dashboardQuery(month))
  const runs = useQuery(runsQuery(month))
  const source = useQuery(activitySourceQuery())
  const importRuns = useImportRuns(month)

  if (!dashboard.data || !runs.data) {
    return null
  }
  return (
    <DashboardView
      dashboard={dashboard.data}
      runs={runs.data}
      dummySource={source.data?.id === 'dummy'}
      elapsedDays={elapsedDays(month)}
      onMonthChange={(next) => {
        importRuns.reset()
        void navigate({ search: { month: next } })
      }}
      onImport={() => importRuns.mutate()}
      importing={importRuns.isPending}
      lastImport={importRuns.data}
      importError={importRuns.error ? describeImportError(importRuns.error) : undefined}
    />
  )
}
