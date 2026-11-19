import type { NewRun } from '../api/client'
import { formatDate } from './format'

/** Used until the backend has said how often to ask (runbuddy.activity.check-interval). */
const DEFAULT_INTERVAL_SECONDS = 180
/** After a failure (rate limit, expired key, network), ask this many times less often. */
const BACKOFF = 4

/** What the header's notice says (#40): the day of one new run, or how many there are. */
export function describeNewRuns(newRuns: NewRun[]): string {
  const [only] = newRuns
  return newRuns.length === 1 && only ? `${formatDate(only.date)} のランを取り込む` : `新しいラン ${newRuns.length} 件を取り込む`
}

/** When to check for new runs again, given the interval the backend sent and whether the last check failed. */
export function nextCheckMillis(intervalSeconds: number | undefined, failed: boolean): number {
  const seconds = intervalSeconds ?? DEFAULT_INTERVAL_SECONDS
  return seconds * 1000 * (failed ? BACKOFF : 1)
}
