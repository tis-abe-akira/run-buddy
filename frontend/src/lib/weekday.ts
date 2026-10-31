/** Saturdays and Sundays are the long-run and key-workout days, so the UI singles them out. */
export type Weekday = { label: string; isWeekend: boolean }

const LABELS = '日月火水木金土'

/** "2026-11-01" (or a date-time) -> { label: '日', isWeekend: true }; null while a date is incomplete. */
export function weekdayOf(iso: string): Weekday | null {
  const date = new Date(`${iso.slice(0, 10)}T00:00:00`)
  if (Number.isNaN(date.getTime())) {
    return null
  }
  const day = date.getDay()
  return { label: LABELS[day]!, isWeekend: day === 0 || day === 6 }
}

export function isWeekend(iso: string): boolean {
  return weekdayOf(iso)?.isWeekend ?? false
}
