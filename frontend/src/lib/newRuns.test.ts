import { describeNewRuns, nextCheckMillis } from './newRuns'

describe('describeNewRuns', () => {
  it('names the day of a single new run', () => {
    expect(describeNewRuns([{ activityId: 'g-1', date: '2026-11-19' }])).toBe('11/19 のランを取り込む')
  })

  it('counts several', () => {
    expect(
      describeNewRuns([
        { activityId: 'g-3', date: '2026-11-19' },
        { activityId: 'g-2', date: '2026-11-18' },
      ]),
    ).toBe('新しいラン 2 件を取り込む')
  })
})

describe('nextCheckMillis', () => {
  it('asks again when the backend says to', () => {
    expect(nextCheckMillis(180, false)).toBe(180_000)
    expect(nextCheckMillis(60, false)).toBe(60_000)
  })

  it('uses three minutes before the first answer', () => {
    expect(nextCheckMillis(undefined, false)).toBe(180_000)
  })

  /** A failing Garmin (rate limit, expired key) is asked less often until it answers again. */
  it('waits four times as long after a failure', () => {
    expect(nextCheckMillis(180, true)).toBe(720_000)
    expect(nextCheckMillis(undefined, true)).toBe(720_000)
  })
})
