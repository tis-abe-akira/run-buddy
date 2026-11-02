import type { Race } from '../api/client'

/** Distances offered in the race form; any other distance can be typed in. */
export const DISTANCE_PRESETS: { label: string; km: number }[] = [
  { label: 'フルマラソン', km: 42.195 },
  { label: 'ハーフマラソン', km: 21.0975 },
  { label: '10 km', km: 10 },
  { label: '5 km', km: 5 },
]

/** "フルマラソン" for 42.195 km and the other presets; otherwise the distance, e.g. "30 km". */
export function formatRaceDistance(km: number): string {
  return DISTANCE_PRESETS.find((p) => p.km === km)?.label ?? `${km} km`
}

export type FinishTime = { ok: true; seconds: number | null } | { ok: false }

const TIME_PATTERN = /^(\d{1,2}):([0-5]\d)(?::([0-5]\d))?$/

/** "5:00:00" or "4:59" -> seconds; empty means the goal is just to finish. */
export function parseFinishTime(text: string): FinishTime {
  const trimmed = text.trim()
  if (trimmed === '') {
    return { ok: true, seconds: null }
  }
  const match = TIME_PATTERN.exec(trimmed)
  if (!match) {
    return { ok: false }
  }
  const seconds = Number(match[1]) * 3600 + Number(match[2]) * 60 + Number(match[3] ?? 0)
  return seconds > 0 ? { ok: true, seconds } : { ok: false }
}

/** The earliest race from today on (the API lists races earliest first). */
export function nextRace(races: Race[]): Race | undefined {
  return races.find((race) => race.daysUntil >= 0)
}

export function formatDaysLeft(days: number): string {
  if (days === 0) {
    return '今日'
  }
  if (days === 1) {
    return '明日'
  }
  return `あと ${days} 日`
}

/** Always "h:mm:ss" (e.g. "0:25:00"), so the form reads back what it shows. */
export function formatFinishTime(seconds: number): string {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

/** "目標 5:00:00 · 7:06/km", or "完走" when the goal is to finish. */
export function formatGoal(race: Pick<Race, 'targetSeconds' | 'targetPaceSecondsPerKm'>): string {
  if (race.targetSeconds === null) {
    return '完走'
  }
  const pace = race.targetPaceSecondsPerKm
  return `目標 ${formatFinishTime(race.targetSeconds)}${pace === null ? '' : ` · ${Math.floor(pace / 60)}:${String(pace % 60).padStart(2, '0')}/km`}`
}
