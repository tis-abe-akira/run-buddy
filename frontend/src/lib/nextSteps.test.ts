import type { NextStepCard } from '../api/client'
import { describeNextStep } from './nextSteps'

const THIS_MONTH = '2026-11'

describe('describeNextStep', () => {
  it('asks for a review of the plan when the runner is behind, with the daily distance that still reaches the target', () => {
    const card: NextStepCard = { kind: 'BEHIND_TARGET', month: '2026-11', shortfallKm: 30, remainingKm: 80, daysLeft: 10 }

    const step = describeNextStep(card, THIS_MONTH)

    expect(step.title).toBe('目標まであと 80.0 km')
    expect(step.body).toBe('今のペースより 30.0 km 遅れています。残り 10 日で、1日あたり 8.0 km です。')
    expect(step.actions).toEqual([
      { label: '計画を見直す', to: 'advisor', month: '2026-11', ask: expect.stringContaining('あと 80.0 km、残り 10 日') },
    ])
  })

  it('suggests tapering before a race and asks the advisor to adjust the plan', () => {
    const card: NextStepCard = {
      kind: 'RACE_APPROACHING',
      raceId: 'r-1',
      raceName: '秋のマラソン',
      raceDate: '2026-12-06',
      distanceKm: 42.195,
      daysToGo: 11,
    }

    const step = describeNextStep(card, THIS_MONTH)

    expect(step.title).toBe('秋のマラソンまであと 11 日')
    expect(step.body).toContain('テーパリング')
    expect(step.actions).toHaveLength(1)
    expect(step.actions[0]).toMatchObject({ to: 'advisor', month: THIS_MONTH })
    expect(step.actions[0]).toHaveProperty('ask', expect.stringContaining('テーパリング'))
  })

  it('only cheers on race day', () => {
    const card: NextStepCard = {
      kind: 'RACE_APPROACHING',
      raceId: 'r-1',
      raceName: '秋のマラソン',
      raceDate: '2026-11-06',
      distanceKm: 42.195,
      daysToGo: 0,
    }

    const step = describeNextStep(card, THIS_MONTH)

    expect(step.title).toBe('今日は秋のマラソンの日です')
    expect(step.actions).toEqual([])
  })

  it('reminds that runs may just not be imported yet, after a week without running', () => {
    const card: NextStepCard = { kind: 'NO_RUNS_LATELY', lastRunDate: '2026-11-10', daysSince: 15 }

    const step = describeNextStep(card, THIS_MONTH)

    expect(step.title).toBe('最後のランから 15 日')
    expect(step.body).toContain('最新を取り込む')
    expect(step.actions[0]).toMatchObject({ to: 'advisor', month: THIS_MONTH })
  })

  it('offers to plan next month by hand or with the advisor', () => {
    const card: NextStepCard = { kind: 'MONTH_ENDING', nextMonth: '2026-12', daysLeft: 5 }

    const step = describeNextStep(card, THIS_MONTH)

    expect(step.title).toBe('2026年12月の計画がまだです')
    expect(step.body).toBe('今月はあと 5 日です。')
    expect(step.actions).toEqual([
      { label: '計画を作る', to: 'plan', month: '2026-12' },
      { label: 'AI と作る', to: 'advisor', month: '2026-12', ask: '2026年12月の計画を作ってください。' },
    ])
  })

  it('says the month ends today on its last day', () => {
    const step = describeNextStep({ kind: 'MONTH_ENDING', nextMonth: '2026-12', daysLeft: 0 }, THIS_MONTH)

    expect(step.body).toBe('今日で今月が終わります。')
  })
})
