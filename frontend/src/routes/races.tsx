import { useQuery } from '@tanstack/react-query'
import { createFileRoute } from '@tanstack/react-router'
import { racesQuery, useRaceMutations } from '../api/queries'
import { ErrorBand } from '../components/ErrorBand'
import { RacesView } from '../components/RacesView'

export const Route = createFileRoute('/races')({
  loader: ({ context }) => context.queryClient.ensureQueryData(racesQuery()),
  component: RacesPage,
  errorComponent: ({ error }) => <ErrorBand error={error} />,
})

function RacesPage() {
  const races = useQuery(racesQuery())
  const { create, update, remove } = useRaceMutations()
  const failed = create.error ?? update.error ?? remove.error

  if (!races.data) {
    return null
  }
  return (
    <>
      <section className="band">
        <div className="container stack" style={{ gap: 'var(--spacing-xs)' }}>
          <span className="caption-uppercase muted">Races</span>
          <h1 className="display-lg">目標のレース</h1>
        </div>
      </section>
      <section className="band">
        <div className="container">
          <RacesView
            races={races.data}
            onCreate={(race) => create.mutate(race)}
            onUpdate={(id, race) => update.mutate({ id, race })}
            onDelete={(id) => remove.mutate(id)}
            saving={create.isPending || update.isPending || remove.isPending}
            error={failed ? `保存できませんでした：${failed.message}` : undefined}
          />
        </div>
      </section>
    </>
  )
}
