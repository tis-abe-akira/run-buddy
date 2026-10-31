import { useQuery } from '@tanstack/react-query'
import { createFileRoute, Link } from '@tanstack/react-router'
import { useState } from 'react'
import { ApiError, type AdvisorInfo, type ProposedPlan } from '../api/client'
import { advisorProvidersQuery, useConsult, useSavePlan } from '../api/queries'
import { AdvisorChat, type Adoption, type ChatEntry } from '../components/AdvisorChat'
import { ErrorBand } from '../components/ErrorBand'
import { MonthSwitcher } from '../components/MonthSwitcher'
import { currentMonth, formatMonth, isMonth, shiftMonth, type Month } from '../lib/month'

type AdvisorSearch = { month?: Month }

export const Route = createFileRoute('/advisor')({
  validateSearch: (search: Record<string, unknown>): AdvisorSearch => (isMonth(search.month) ? { month: search.month } : {}),
  loader: ({ context }) => context.queryClient.ensureQueryData(advisorProvidersQuery()),
  // A new month starts a new conversation.
  remountDeps: ({ search }) => search.month,
  component: AdvisorPage,
  errorComponent: ({ error }) => <ErrorBand error={error} />,
})

function describeError(error: Error): string {
  if (error instanceof ApiError && error.status === 429) {
    return 'AI の利用上限に達しました。1分ほど待ってから送り直すか、別のモデルを選んでください。'
  }
  return `回答を得られませんでした: ${error.message}`
}

/** Real providers first; the rule-based fake only when nothing else is configured. */
function defaultProvider(providers: AdvisorInfo[]): AdvisorInfo | undefined {
  return providers.find((p) => p.id !== 'fake') ?? providers[0]
}

function AdvisorPage() {
  const { month: requested } = Route.useSearch()
  const month = requested ?? shiftMonth(currentMonth(), 1)
  const navigate = Route.useNavigate()
  const providers = useQuery(advisorProvidersQuery()).data ?? []
  const initial = defaultProvider(providers)
  const [choice, setChoice] = useState({ provider: initial?.id ?? '', model: initial?.models[0] ?? '' })
  const [entries, setEntries] = useState<ChatEntry[]>([])
  const [adoption, setAdoption] = useState<Adoption>()
  const consult = useConsult()
  const savePlan = useSavePlan(month)

  function send(text: string) {
    const history: ChatEntry[] = [...entries, { speaker: 'USER', text }]
    setEntries(history)
    const asked = choice
    consult.mutate(
      { ...asked, month, messages: history.map((e) => ({ speaker: e.speaker, text: e.text })) },
      {
        onSuccess: (advice) =>
          setEntries((current) => [...current, { speaker: 'ADVISOR', text: advice.reply, ...asked, proposal: advice.proposedPlan }]),
      },
    )
  }

  function adopt(index: number) {
    const entry = entries[index]
    if (entry?.speaker !== 'ADVISOR' || !entry.proposal) {
      return
    }
    const proposal: ProposedPlan = entry.proposal
    setAdoption({ index, state: 'saving' })
    savePlan.mutate(
      { targetKm: proposal.targetKm, workouts: proposal.workouts },
      {
        onSuccess: () => setAdoption({ index, state: 'saved' }),
        onError: (e) => setAdoption({ index, state: 'error', message: e.message }),
      },
    )
  }

  function reset() {
    setEntries([])
    setAdoption(undefined)
    consult.reset()
  }

  return (
    <>
      <section className="band">
        <div className="container row" style={{ alignItems: 'flex-end', gap: 'var(--spacing-lg)' }}>
          <div className="stack" style={{ gap: 'var(--spacing-xs)' }}>
            <span className="caption-uppercase muted">AI coach</span>
            <h1 className="display-lg">{formatMonth(month)}のプランを相談</h1>
          </div>
          <span className="spacer" />
          <MonthSwitcher month={month} onChange={(next) => void navigate({ search: { month: next } })} />
        </div>
      </section>
      <section className="band">
        <div className="container stack">
          {providers.length === 0 ? (
            <div className="feature-card body-sm">
              利用できる AI プロバイダがありません。backend で API キー（例: GEMINI_API_KEY）を設定してください。
            </div>
          ) : (
            <AdvisorChat
              month={month}
              providers={providers}
              provider={choice.provider}
              model={choice.model}
              onChoose={(provider, model) => setChoice({ provider, model })}
              entries={entries}
              pending={consult.isPending}
              error={consult.error ? describeError(consult.error) : undefined}
              onSend={send}
              onAdopt={adopt}
              adoption={adoption}
              onReset={reset}
            />
          )}
          {adoption?.state === 'saved' && (
            <Link to="/plans/$month" params={{ month }} className="text-link body-sm">
              採用したプランを開く →
            </Link>
          )}
        </div>
      </section>
    </>
  )
}
