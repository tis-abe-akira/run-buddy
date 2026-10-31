import type { Month } from './month'
import { weekdayOf } from './weekday'

/** A calendar day in ISO form, e.g. "2026-11-08". Matches java.time.LocalDate on the backend. */
export type IsoDate = string

const DATE_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/

/** Whether the value is a real calendar date ("2026-02-29" is not). */
export function isIsoDate(value: unknown): value is IsoDate {
  if (typeof value !== 'string') {
    return false
  }
  const match = DATE_PATTERN.exec(value)
  if (!match) {
    return false
  }
  const [year, month, day] = [Number(match[1]), Number(match[2]), Number(match[3])]
  const date = new Date(Date.UTC(year, month - 1, day))
  return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day
}

/** The date `delta` days later (or earlier, when negative). */
export function shiftDay(date: IsoDate, delta: number): IsoDate {
  const shifted = new Date(`${date}T00:00:00Z`)
  shifted.setUTCDate(shifted.getUTCDate() + delta)
  return shifted.toISOString().slice(0, 10)
}

export function monthOfDate(date: IsoDate): Month {
  return date.slice(0, 7)
}

/** "2026-11-08" -> "11月8日（日）" */
export function formatDayHeading(date: IsoDate): string {
  return `${Number(date.slice(5, 7))}月${Number(date.slice(8, 10))}日（${weekdayOf(date)?.label ?? ''}）`
}

/** "2026-11-08T06:12:41" -> "6:12": the wall-clock time where the run started, as stored. */
export function formatStartTime(startedAt: string): string {
  return `${Number(startedAt.slice(11, 13))}:${startedAt.slice(14, 16)}`
}
