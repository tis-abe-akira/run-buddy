/** 213.12 -> "213.1" */
export function formatKm(km: number): string {
  return km.toLocaleString('ja-JP', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
}

/** 339 -> "5:39" */
export function formatPace(secondsPerKm: number | null): string {
  if (secondsPerKm === null) {
    return '—'
  }
  const minutes = Math.floor(secondsPerKm / 60)
  const seconds = secondsPerKm % 60
  return `${minutes}:${String(seconds).padStart(2, '0')}`
}

/** 3923 -> "1:05:23", 1500 -> "25:00" */
export function formatDuration(totalSeconds: number): string {
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  const mm = String(minutes).padStart(hours > 0 ? 2 : 1, '0')
  const ss = String(seconds).padStart(2, '0')
  return hours > 0 ? `${hours}:${mm}:${ss}` : `${mm}:${ss}`
}

/** 0.714 -> "71%" */
export function formatRate(rate: number | null): string {
  return rate === null ? '—' : `${Math.round(rate * 100)}%`
}

/** "2026-11-03" or "2026-11-03T06:30:00" -> "11/3（火）" */
export function formatDay(iso: string): string {
  const date = new Date(`${iso.slice(0, 10)}T00:00:00`)
  const weekday = '日月火水木金土'[date.getDay()]
  return `${date.getMonth() + 1}/${date.getDate()}（${weekday}）`
}
