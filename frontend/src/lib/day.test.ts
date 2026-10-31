import { formatDayHeading, formatStartTime, isIsoDate, monthOfDate, shiftDay } from './day'

describe('isIsoDate', () => {
  it('accepts real calendar dates only', () => {
    expect(isIsoDate('2026-11-08')).toBe(true)
    expect(isIsoDate('2028-02-29')).toBe(true)
    expect(isIsoDate('2026-02-29')).toBe(false)
    expect(isIsoDate('2026-13-01')).toBe(false)
    expect(isIsoDate('2026-11-8')).toBe(false)
    expect(isIsoDate(undefined)).toBe(false)
  })
})

describe('shiftDay', () => {
  it('crosses month and year ends', () => {
    expect(shiftDay('2026-11-08', 1)).toBe('2026-11-09')
    expect(shiftDay('2026-11-30', 1)).toBe('2026-12-01')
    expect(shiftDay('2027-01-01', -1)).toBe('2026-12-31')
  })
})

describe('monthOfDate', () => {
  it('is the month the day belongs to', () => {
    expect(monthOfDate('2026-11-08')).toBe('2026-11')
  })
})

describe('formatDayHeading', () => {
  it('names the date and its weekday', () => {
    expect(formatDayHeading('2026-11-08')).toBe('11月8日（日）')
  })
})

describe('formatStartTime', () => {
  it('is the local wall-clock time of the start', () => {
    expect(formatStartTime('2026-11-08T06:12:41')).toBe('6:12')
    expect(formatStartTime('2026-11-08T18:05:00')).toBe('18:05')
  })
})
