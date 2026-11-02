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
      ]),
  })
}

export function useSavePlan(month: Month) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (plan: Plan) => api.savePlan(month, plan),
    onSuccess: (saved) => {
      queryClient.setQueryData(planQuery(month).queryKey, saved)
      return queryClient.invalidateQueries({ queryKey: ['dashboard', month] })
    },
  })
}

export const advisorProvidersQuery = () =>
  queryOptions({ queryKey: ['advisor', 'providers'], queryFn: api.advisorProviders, staleTime: Infinity })

export function useConsult() {
  return useMutation({ mutationFn: (consultation: ConsultationRequest) => api.consult(consultation) })
}

export const racesQuery = () => queryOptions({ queryKey: ['races'], queryFn: api.races })

export function useRaceMutations() {
  const queryClient = useQueryClient()
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['races'] })
  return {
    create: useMutation({ mutationFn: (race: RaceInput) => api.createRace(race), onSuccess: refresh }),
    update: useMutation({
      mutationFn: ({ id, race }: { id: string; race: RaceInput }) => api.updateRace(id, race),
      onSuccess: refresh,
    }),
    remove: useMutation({ mutationFn: (id: string) => api.deleteRace(id), onSuccess: refresh }),
  }
}
