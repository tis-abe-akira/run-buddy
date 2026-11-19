import { useQuery, type QueryClient } from '@tanstack/react-query'
import { createRootRouteWithContext, Outlet, useNavigate } from '@tanstack/react-router'
import { newRunsQuery, useImportLatest } from '../api/queries'
import { Footer } from '../components/Footer'
import { NewRunsNotice } from '../components/NewRunsNotice'
import { TopNav } from '../components/TopNav'
import { describeImportError } from '../lib/importError'

export const Route = createRootRouteWithContext<{ queryClient: QueryClient }>()({
  component: () => (
    <>
      <TopNav aside={<NewRunsLoader />} />
      <main>
        <Outlet />
      </main>
      <Footer />
    </>
  ),
  notFoundComponent: () => (
    <section className="band">
      <div className="container stack">
        <h1 className="display-md">ページが見つかりません</h1>
      </div>
    </section>
  ),
})

/**
 * Checks for new runs every few minutes and offers them in the header (#40). Importing opens the day
 * of the newest run imported, where its review, course and laps are. A failed check shows nothing:
 * the dashboard's import button still says what is wrong.
 */
function NewRunsLoader() {
  const check = useQuery(newRunsQuery())
  const importLatest = useImportLatest()
  const navigate = useNavigate()
  return (
    <NewRunsNotice
      newRuns={check.data?.newRuns ?? []}
      importing={importLatest.isPending}
      error={importLatest.error ? describeImportError(importLatest.error) : undefined}
      onImport={() =>
        importLatest.mutate(undefined, {
          onSuccess: (result) => {
            const newest = result.newRuns.at(-1)
            if (newest) {
              void navigate({ to: '/days/$date', params: { date: newest.date } })
            }
          },
        })
      }
    />
  )
}
