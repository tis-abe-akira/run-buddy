import type { Run } from '../api/client'

/** A run for component tests: 8 km easy on 2 Nov 2026, no optional measurements, unless overridden. */
export function aRun(overrides: Partial<Run> = {}): Run {
  return {
    activityId: 'run-1',
    startedAt: '2026-11-02T06:30:00',
    distanceKm: 8,
    durationSeconds: 2640,
    paceSecondsPerKm: 330,
    averageHeartRate: null,
    maxHeartRate: null,
    averageCadence: null,
    maxCadence: null,
    averageStrideLengthCm: null,
    elevationGainMeters: null,
    ...overrides,
  }
}
