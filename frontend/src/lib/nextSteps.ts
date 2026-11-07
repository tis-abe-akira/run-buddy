import type { NextStepCard } from '../api/client'
import { formatDay, formatKm } from './format'
import { formatMonth, type Month } from './month'
import { formatDaysLeft, formatRaceDistance } from './race'

/**
 * Where a card's button goes. The advisor opens with `ask` already in the message box; nothing is sent
 * or saved until the runner does it (#26).
 */
export type NextStepAction =
  | { label: string; to: 'advisor'; month: Month; ask: string }
  | { label: string; to: 'plan'; month: Month }

export type NextStep = { title: string; body: string; actions: NextStepAction[] }

/** The words and buttons for a card from GET /api/next-steps. `thisMonth` is today's month. */
export function describeNextStep(card: NextStepCard, thisMonth: Month): NextStep {
  switch (card.kind) {
    case 'RACE_APPROACHING': {
      if (card.daysToGo === 0) {
        return { title: `今日は${card.raceName}の日です`, body: 'ここまでの練習を信じて、楽しんで走りましょう。', actions: [] }
      }
      const when = formatDaysLeft(card.daysToGo)
      return {
        title: `${card.raceName}まで${when}`,
        body: 'テーパリングの時期です。量を落として、疲れを抜きましょう。',
        actions: [
          {
            label: 'AI に調整を相談',
            to: 'advisor',
            month: thisMonth,
            ask: `${card.raceName}（${formatDay(card.raceDate)}、${formatRaceDistance(card.distanceKm)}）まで${when}です。テーパリングに入るよう、残りの計画を調整してください。`,
          },
        ],
      }
    }
    case 'BEHIND_TARGET': {
      const remaining = formatKm(card.remainingKm)
      return {
        title: `目標まであと ${remaining} km`,
        body: `今のペースより ${formatKm(card.shortfallKm)} km 遅れています。残り ${card.daysLeft} 日で、1日あたり ${formatKm(card.remainingKm / card.daysLeft)} km です。`,
        actions: [
          {
            label: '計画を見直す',
            to: 'advisor',
            month: card.month as Month,
            ask: `今月の目標まであと ${remaining} km、残り ${card.daysLeft} 日です。届くように残りの計画を見直してください。難しければ、目標の下げ方も提案してください。`,
          },
        ],
      }
    }
    case 'NO_RUNS_LATELY':
      return {
        title: `最後のランから ${card.daysSince} 日`,
        body: `${formatDay(card.lastRunDate)}から走っていません。取り込みがまだなら「最新を取り込む」を。休んでいたなら、軽いランから再開しましょう。`,
        actions: [
          {
            label: '再開の計画を相談',
            to: 'advisor',
            month: thisMonth,
            ask: `${card.daysSince} 日走っていません。無理なく再開できるように、これからの計画を提案してください。`,
          },
        ],
      }
    case 'MONTH_ENDING': {
      const next = card.nextMonth as Month
      return {
        title: `${formatMonth(next)}の計画がまだです`,
        body: card.daysLeft === 0 ? '今日で今月が終わります。' : `今月はあと ${card.daysLeft} 日です。`,
        actions: [
          { label: '計画を作る', to: 'plan', month: next },
          { label: 'AI と作る', to: 'advisor', month: next, ask: `${formatMonth(next)}の計画を作ってください。` },
        ],
      }
    }
  }
}
