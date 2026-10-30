import type { components } from './schema.gen'
import type { Month } from '../lib/month'

type Schemas = components['schemas']

export type Dashboard = Schemas['DashboardResponse']
export type DashboardDay = Schemas['DayBody']
export type Run = Schemas['RunResponse']
export type Plan = Schemas['PlanBody']
export type Workout = Schemas['WorkoutBody']
export type WorkoutType = Workout['type']
export type ImportResult = Schemas['ImportResult']

/** An error response from the backend, carrying the RFC 9457 problem detail when there is one. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly detail: string | undefined,
  ) {
    super(detail ?? `request failed with status ${status}`)
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: { Accept: 'application/json', ...(init?.body ? { 'Content-Type': 'application/json' } : {}) },
  })
  if (!response.ok) {
    const problem = (await response.json().catch(() => undefined)) as { detail?: string } | undefined
    throw new ApiError(response.status, problem?.detail)
  }
  return (await response.json()) as T
}

export const api = {
  dashboard: (month: Month) => request<Dashboard>(`/api/dashboard/${month}`),
  runs: (month: Month) => request<Run[]>(`/api/runs/${month}`),
  importRuns: (month: Month) => request<ImportResult>(`/api/runs/${month}/import`, { method: 'POST' }),
  plan: (month: Month) => request<Plan>(`/api/plans/${month}`),
  savePlan: (month: Month, plan: Plan) =>
    request<Plan>(`/api/plans/${month}`, { method: 'PUT', body: JSON.stringify(plan) }),
}
