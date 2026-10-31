import type { Month } from '../lib/month'

/** Downloads one month of plan and actual runs as a day-by-day CSV (served by the backend). */
export function ExportLink({ month }: { month: Month }) {
  return (
    <a href={exportCsvUrl(month)} download className="button button-secondary">
      CSV でエクスポート
    </a>
  )
}

export function exportCsvUrl(month: Month): string {
  return `/api/export/${month}.csv`
}
