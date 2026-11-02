import type { Race } from '../api/client'

/** A race for component tests: a marathon 21 days from now aiming for sub-5, unless overridden. */
export function aRace(overrides: Partial<Race> = {}): Race {
  return {
    id: 'race-1',
    name: 'Autumn marathon',
    date: '2026-11-22',
    distanceKm: 42.195,
    targetSeconds: 18000,
    targetPaceSecondsPerKm: 426,
    daysUntil: 21,
    ...overrides,
  }
}
