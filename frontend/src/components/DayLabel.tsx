import { formatDate } from '../lib/format'
import { weekdayOf } from '../lib/weekday'

/** "11/7（土）", with Saturdays and Sundays emphasised. */
export function DayLabel({ date }: { date: string }) {
  const weekday = weekdayOf(date)
  return (
    <span className={weekday?.isWeekend ? 'day-label weekend' : 'day-label'}>
      {formatDate(date)}
      <span className="day-weekday">（{weekday?.label}）</span>
    </span>
  )
}

/** A round chip with the weekday next to a date input. */
export function WeekdayChip({ date }: { date: string }) {
  const weekday = weekdayOf(date)
  if (!weekday) {
    return <span className="weekday-chip" aria-hidden="true" />
  }
  return (
    <span className={weekday.isWeekend ? 'weekday-chip weekend' : 'weekday-chip'} title={`${weekday.label}曜日`}>
      {weekday.label}
    </span>
  )
}
