import { Wordmark } from './Wordmark'

export function Footer() {
  return (
    <footer className="footer">
      <div className="container stack">
        <span className="wordmark">
          <Wordmark />
        </span>
        <span>Phase 1 — Garmin連携はダミーデータで動作しています。データはバックエンドの再起動で消えます。</span>
      </div>
    </footer>
  )
}
