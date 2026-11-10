import { queryOptions, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, type ConsultationRequest, type Plan, type RaceInput } from './client'
import type { IsoDate } from '../lib/day'
import type { Month } from '../lib/month'

export const dashboardQuery = (month: Month) =>
  queryOptions({ queryKey: ['dashboard', month], queryFn: () => api.dashboard(month) })

export const runsQuery = (month: Month) =>
  queryOptions({ queryKey: ['runs', month], queryFn: () => api.runs(month) })

export const dayQuery = (date: IsoDate) =>
  queryOptions({ queryKey: ['day', date], queryFn: () => api.day(date) })

/**
 * A run's laps (ADR-0012). The first look downloads the activity file from Garmin, so a failure is
 * not retried behind the runner's back: they press "再試行" instead. Once loaded it does not change.
 */
export const runDetailQuery = (activityId: string) =>
  queryOptions({ queryKey: ['run-detail', activityId], queryFn: () => api.runDetail(activityId), retry: false, staleTime: Infinity })

/** A run's saved AI review (#35); null until it is reviewed. */
export const runReviewQuery = (activityId: string) =>
  queryOptions({ queryKey: ['run-review', activityId], queryFn: () => api.runReview(activityId), staleTime: Infinity })

/** Asks the AI to review a run now; the new review replaces the saved one in the cache. */
export function useReviewRun(activityId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api.reviewRun(activityId),
    onSuccess: (review) => queryClient.setQueryData(runReviewQuery(activityId).queryKey, { review }),
  })
}

export const planQuery = (month: Month) =>
  queryOptions({ queryKey: ['plan', month], queryFn: () => api.plan(month) })

export const activitySourceQuery = () =>
  queryOptions({ queryKey: ['activity-source'], queryFn: api.activitySource, staleTime: Infinity })

export function useImportRuns(month: Month) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api.importRuns(month),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: ['dashboard', month] }),
        queryClient.invalidateQueries({ queryKey: ['runs', month] }),
        queryClient.invalidateQueries({ queryKey: ['next-steps'] }),
      ]),
  })
}

/** Imports what is new since the latest stored run. It can touch any month, so every month's data is refreshed. */
export function useImportLatest() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: api.importLatest,
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
        queryClient.invalidateQueries({ queryKey: ['runs'] }),
        queryClient.invalidateQueries({ queryKey: ['day'] }),
        queryClient.invalidateQueries({ queryKey: ['next-steps'] }),
      ]),
  })
}

export function useSavePlan(month: Month) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (plan: Plan) => api.savePlan(month, plan),
    onSuccess: (saved) => {
      queryClient.setQueryData(planQuery(month).queryKey, saved)
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: ['dashboard', month] }),
        queryClient.invalidateQueries({ queryKey: ['next-steps'] }),
      ])
    },
  })
}

export const advisorProvidersQuery = () =>
  queryOptions({ queryKey: ['advisor', 'providers'], queryFn: api.advisorProviders, staleTime: Infinity })

export function useConsult() {
  return useMutation({ mutationFn: (consultation: ConsultationRequest) => api.consult(consultation) })
}

export const racesQuery = () => queryOptions({ queryKey: ['races'], queryFn: api.races })

/** Cards for today (#27). Anything that changes runs, plans or races refreshes them. */
export const nextStepsQuery = () => queryOptions({ queryKey: ['next-steps'], queryFn: api.nextSteps })

export function useRaceMutations() {
  const queryClient = useQueryClient()
  const refresh = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: ['races'] }),
      queryClient.invalidateQueries({ queryKey: ['next-steps'] }),
    ])
  return {
    create: useMutation({ mutationFn: (race: RaceInput) => api.createRace(race), onSuccess: refresh }),
    update: useMutation({
      mutationFn: ({ id, race }: { id: string; race: RaceInput }) => api.updateRace(id, race),
      onSuccess: refresh,
    }),
    remove: useMutation({ mutationFn: (id: string) => api.deleteRace(id), onSuccess: refresh }),
  }
}
