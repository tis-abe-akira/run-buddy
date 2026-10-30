import { useQuery } from '@tanstack/react-query'
import { createFileRoute, Link, notFound } from '@tanstack/react-router'
import { planQuery, useSavePlan } from '../api/queries'
import { ErrorBand } from '../components/ErrorBand'
import { PlanEditor } from '../components/PlanEditor'
import { formatMonth, isMonth, shiftMonth } from '../lib/month'

export const Route = createFileRoute('/plans/$month')({
  beforeLoad: ({ params }) => {
    if (!isMonth(params.month)) {
      throw notFound()
    }
  },
  // Remount per month so the draft and the save state never leak into another month.
  remountDeps: ({ params }) => params.month,
  loader: ({ context, params }) => context.queryClient.ensureQueryData(planQuery(params.month)),
  component: PlanPage,
  errorComponent: ({ error }) => <ErrorBand error={error} />,
})

function PlanPage() {
  const { month } = Route.useParams()
  const plan = useQuery(planQuery(month))
  const savePlan = useSavePlan(month)

  if (!plan.data) {
    return null
  }
  return (
    <>
      <section className="band">
        <div className="container row" style={{ alignItems: 'flex-end', gap: 'var(--spacing-lg)' }}>
          <div className="stack" style={{ gap: 'var(--spacing-xs)' }}>
            <span className="caption-uppercase muted">Training plan</span>
            <h1 className="display-lg">{formatMonth(month)}のプラン</h1>
          </div>
          <span className="spacer" />
          <div className="row">
            <Link to="/plans/$month" params={{ month: shiftMonth(month, -1) }} className="button-icon" aria-label="前の月">
              ‹
            </Link>
            <Link to="/plans/$month" params={{ month: shiftMonth(month, 1) }} className="button-icon" aria-label="次の月">
              ›
            </Link>
            <Link to="/dashboard" search={{ month }} className="text-link body-sm" style={{ marginLeft: 'var(--spacing-xs)' }}>
              この月のダッシュボードへ
            </Link>
          </div>
        </div>
      </section>
      <section className="band">
        <div className="container">
          <PlanEditor
            month={month}
            initial={plan.data}
            onSave={(next) => savePlan.mutate(next)}
            saving={savePlan.isPending}
            saved={savePlan.isSuccess}
            serverError={savePlan.error?.message}
          />
        </div>
      </section>
    </>
  )
}
