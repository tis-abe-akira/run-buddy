import type { NewRun } from '../api/client'
import { describeNewRuns } from '../lib/newRuns'

type Props = {
  /** Runs in Garmin not imported yet, newest first. */
  newRuns: NewRun[]
  importing: boolean
  /** Why the last import from here failed; the notice stays so it can be pressed again. */
  error?: string
  onImport: () => void
}

/**
 * The header's notice of new runs (#40): seen on every page as soon as the app is opened, instead of
 * a button to look for. Pressing it imports them. Nothing is shown while there is nothing new.
 */
export function NewRunsNotice({ newRuns, importing, error, onImport }: Props) {
  if (newRuns.length === 0 && !importing && !error) {
    return null
  }
  const label = describeNewRuns(newRuns)
  return (
    <div className="new-runs" aria-live="polite">
      {(newRuns.length > 0 || importing) && (
        <button
          type="button"
          className="new-runs-notice"
          disabled={importing}
          onClick={onImport}
        >
          <span className="new-runs-dot" aria-hidden="true" />
          {importing ? (
            '取り込み中…'
          ) : (
            label
          )}
        </button>
      )}
      {error && (
        <span className="new-runs-error field-error" role="alert">
          {error}
        </span>
      )}
    </div>
  )
}
