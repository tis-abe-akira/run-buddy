import type { ImportResult, LatestImportResult } from '../api/client'
import { formatDate } from '../lib/format'
import type { Month } from '../lib/month'

type Props = {
  month: Month
  onImportLatest: () => void
  onImportMonth: () => void
  /** Which import is running, if any. */
  importing: 'latest' | 'month' | null
  lastLatest?: LatestImportResult
  lastMonth?: ImportResult
  error?: string
}

/**
 * Importing from Garmin. The main action fetches what is new since the latest stored run, whatever
 * month it is in; re-reading the shown month picks up runs edited in Garmin Connect.
 */
export function ImportControls({ month, onImportLatest, onImportMonth, importing, lastLatest, lastMonth, error }: Props) {
  const monthLabel = `${Number(month.slice(5, 7))}月`
  return (
    <>
      <button type="button" className="button button-primary" onClick={onImportLatest} disabled={importing !== null}>
        {importing === 'latest' ? '取り込み中…' : '最新を取り込む'}
      </button>
      <button type="button" className="button button-secondary" onClick={onImportMonth} disabled={importing !== null}>
        {importing === 'month' ? '取り込み中…' : `${monthLabel}を取り込み直す`}
      </button>
      {importing === null && lastLatest && (
        <span className="body-sm muted" role="status">
          {lastLatest.added === 0
            ? `${formatDate(lastLatest.from)}〜${formatDate(lastLatest.to)} に新しいランはありませんでした`
            : `${formatDate(lastLatest.from)}〜${formatDate(lastLatest.to)} を確認し、新しく ${lastLatest.added} 件を取り込みました`}
        </span>
      )}
      {importing === null && !lastLatest && lastMonth && (
        <span className="body-sm muted" role="status">
          {`${Number(lastMonth.month.slice(5, 7))}月の ${lastMonth.fetched} 件を確認し、新しく ${lastMonth.added} 件を取り込みました`}
        </span>
      )}
      {error && (
        <span className="field-error" role="alert">
          {error}
        </span>
      )}
    </>
  )
}
