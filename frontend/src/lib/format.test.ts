import { formatDay, formatDuration, formatKm, formatPace, formatRate } from './format'

describe('format', () => {
  it('shows kilometers with one decimal', () => {
    expect(formatKm(213.12)).toBe('213.1')
    expect(formatKm(0)).toBe('0.0')
    expect(formatKm(1234.56)).toBe('1,234.6')
  })

  it('shows pace as m:ss and a dash when unknown', () => {
    expect(formatPace(339)).toBe('5:39')
    expect(formatPace(300)).toBe('5:00')
    expect(formatPace(null)).toBe('—')
  })

  it('shows durations with hours only when needed', () => {
    expect(formatDuration(1500)).toBe('25:00')
    expect(formatDuration(3923)).toBe('1:05:23')
  })

  it('rounds rates to whole percent', () => {
    expect(formatRate(0.714)).toBe('71%')
    expect(formatRate(1.5)).toBe('150%')
    expect(formatRate(null)).toBe('—')
  })

  it('shows a day with its weekday', () => {
    expect(formatDay('2026-11-03')).toBe('11/3（火）')
    expect(formatDay('2026-11-08T06:30:00')).toBe('11/8（日）')
  })
})
