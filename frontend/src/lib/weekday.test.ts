import { isWeekend, weekdayOf } from './weekday'

describe('weekdayOf', () => {
  it('names the weekday and flags Saturdays and Sundays', () => {
    expect(weekdayOf('2026-11-01')).toEqual({ label: '日', isWeekend: true })
    expect(weekdayOf('2026-11-02')).toEqual({ label: '月', isWeekend: false })
    expect(weekdayOf('2026-11-06')).toEqual({ label: '金', isWeekend: false })
    expect(weekdayOf('2026-11-07')).toEqual({ label: '土', isWeekend: true })
  })

  it('reads the date part of a date-time', () => {
    expect(weekdayOf('2026-11-08T06:30:00')?.label).toBe('日')
  })

  it('has no weekday while a date is still being typed', () => {
    expect(weekdayOf('')).toBeNull()
    expect(isWeekend('')).toBe(false)
  })
})
