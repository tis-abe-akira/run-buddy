import { elapsedDays } from './elapsed'

describe('elapsedDays', () => {
  const now = new Date(2026, 9, 30) // 2026-10-30

  it('counts every day of a past month', () => {
    expect(elapsedDays('2026-09', now)).toBe(30)
  })

  it('counts up to today in the current month', () => {
    expect(elapsedDays('2026-10', now)).toBe(30)
  })

  it('counts nothing in a future month', () => {
    expect(elapsedDays('2026-11', now)).toBe(0)
  })
})
