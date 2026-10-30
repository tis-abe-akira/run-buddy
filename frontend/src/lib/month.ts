/** A calendar month in ISO form, e.g. "2026-11". Matches java.time.YearMonth on the backend. */
export type Month = string

const MONTH_PATTERN = /^(\d{4})-(0[1-9]|1[0-2])$/

export function isMonth(value: unknown): value is Month {
  return typeof value === 'string' && MONTH_PATTERN.test(value)
}

export function currentMonth(now: Date = new Date()): Month {
  return toMonth(now.getFullYear(), now.getMonth() + 1)
}

export function shiftMonth(month: Month, delta: number): Month {
  const [year, monthValue] = parts(month)
  const index = year * 12 + (monthValue - 1) + delta
  return toMonth(Math.floor(index / 12), (index % 12) + 1)
}

export function daysInMonth(month: Month): number {
  const [year, monthValue] = parts(month)
  return new Date(year, monthValue, 0).getDate()
}

/** "2026年11月" */
export function formatMonth(month: Month): string {
  const [year, monthValue] = parts(month)
  return `${year}年${monthValue}月`
}

/** Whether an ISO date ("2026-11-03") falls inside the month. */
export function containsDate(month: Month, isoDate: string): boolean {
  return isoDate.startsWith(`${month}-`) && Number(isoDate.slice(8, 10)) <= daysInMonth(month)
}

function parts(month: Month): [number, number] {
  const match = MONTH_PATTERN.exec(month)
  if (!match) {
    throw new Error(`not a month: ${month}`)
  }
  return [Number(match[1]), Number(match[2])]
}

function toMonth(year: number, monthValue: number): Month {
  return `${String(year).padStart(4, '0')}-${String(monthValue).padStart(2, '0')}`
}
