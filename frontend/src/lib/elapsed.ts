import { currentMonth, daysInMonth, type Month } from './month'

/** How many days of the month have started by `now` (0 for a future month). */
export function elapsedDays(month: Month, now: Date = new Date()): number {
  const today = currentMonth(now)
  if (month < today) {
    return daysInMonth(month)
  }
  return month === today ? now.getDate() : 0
}
