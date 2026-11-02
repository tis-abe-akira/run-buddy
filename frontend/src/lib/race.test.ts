import { aRace } from '../test/races'
import { formatDaysLeft, formatFinishTime, formatGoal, formatRaceDistance, nextRace, parseFinishTime } from './race'

describe('parseFinishTime', () => {
  it('reads hours, minutes and optional seconds', () => {
    expect(parseFinishTime('5:00:00')).toEqual({ ok: true, seconds: 18000 })
    expect(parseFinishTime('4:59')).toEqual({ ok: true, seconds: 17940 })
    expect(parseFinishTime(' 1:05:30 ')).toEqual({ ok: true, seconds: 3930 })
  })

  it('treats an empty field as "just finish"', () => {
    expect(parseFinishTime('')).toEqual({ ok: true, seconds: null })
    expect(parseFinishTime('   ')).toEqual({ ok: true, seconds: null })
  })

  it('rejects what is not a time', () => {
    expect(parseFinishTime('5時間')).toEqual({ ok: false })
    expect(parseFinishTime('5:60:00')).toEqual({ ok: false })
    expect(parseFinishTime('0:00:00')).toEqual({ ok: false })
  })
})

describe('nextRace', () => {
  it('is the earliest race from today on', () => {
    const past = aRace({ id: 'past', daysUntil: -3 })
    const today = aRace({ id: 'today', daysUntil: 0 })
    const later = aRace({ id: 'later', daysUntil: 40 })

    expect(nextRace([past, today, later])?.id).toBe('today')
    expect(nextRace([past])).toBeUndefined()
  })
})

describe('formatDaysLeft', () => {
  it('counts down to race day', () => {
    expect(formatDaysLeft(21)).toBe('あと 21 日')
    expect(formatDaysLeft(1)).toBe('明日')
    expect(formatDaysLeft(0)).toBe('今日')
  })
})

describe('formatFinishTime', () => {
  it('always shows hours so the form reads it back the same', () => {
    expect(formatFinishTime(18000)).toBe('5:00:00')
    expect(formatFinishTime(1500)).toBe('0:25:00')
    expect(parseFinishTime(formatFinishTime(1500))).toEqual({ ok: true, seconds: 1500 })
  })
})

describe('formatGoal', () => {
  it('is the target time and its pace, or finishing', () => {
    expect(formatGoal({ targetSeconds: 18000, targetPaceSecondsPerKm: 426 })).toBe('目標 5:00:00 · 7:06/km')
    expect(formatGoal({ targetSeconds: null, targetPaceSecondsPerKm: null })).toBe('完走')
  })
})

describe('formatRaceDistance', () => {
  it('names the usual distances instead of rounding them', () => {
    expect(formatRaceDistance(42.195)).toBe('フルマラソン')
    expect(formatRaceDistance(21.0975)).toBe('ハーフマラソン')
    expect(formatRaceDistance(30)).toBe('30 km')
  })
})
