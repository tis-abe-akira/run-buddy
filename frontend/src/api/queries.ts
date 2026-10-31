import { queryOptions, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, type ConsultationRequest, type Plan } from './client'
import type { Month } from '../lib/month'

export const dashboardQuery = (month: Month) =>
  queryOptions({ queryKey: ['dashboard', month], queryFn: () => api.dashboard(month) })

export const runsQuery = (month: Month) =>
  queryOptions({ queryKey: ['runs', month], queryFn: () => api.runs(month) })

export const planQuery = (month: Month) =>
  queryOptions({ queryKey: ['plan', month], queryFn: () => api.plan(month) })

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
