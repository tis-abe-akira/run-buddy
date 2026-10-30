import { containsDate, currentMonth, daysInMonth, formatMonth, isMonth, shiftMonth } from './month'

describe('month', () => {
  it('accepts only YYYY-MM', () => {
    expect(isMonth('2026-11')).toBe(true)
    expect(isMonth('2026-13')).toBe(false)
    expect(isMonth('2026-1')).toBe(false)
    expect(isMonth(undefined)).toBe(false)
  })

  it('takes the current month from the local date', () => {
    expect(currentMonth(new Date(2026, 0, 31))).toBe('2026-01')
  })

  it.each([
    ['2026-11', 1, '2026-12'],
    ['2026-12', 1, '2027-01'],
    ['2026-01', -1, '2025-12'],
    ['2026-03', -14, '2025-01'],
  ])('shifts %s by %i to %s', (month, delta, expected) => {
    expect(shiftMonth(month, delta)).toBe(expected)
  })

  it('knows month lengths, including leap years', () => {
    expect(daysInMonth('2026-02')).toBe(28)
    expect(daysInMonth('2028-02')).toBe(29)
    expect(daysInMonth('2026-11')).toBe(30)
  })

  it('formats in Japanese', () => {
    expect(formatMonth('2026-04')).toBe('2026年4月')
  })

  it('checks whether a date belongs to the month', () => {
    expect(containsDate('2026-11', '2026-11-30')).toBe(true)
    expect(containsDate('2026-11', '2026-11-31')).toBe(false)
    expect(containsDate('2026-11', '2026-12-01')).toBe(false)
  })
})
