import { Wordmark } from './Wordmark'

export function Footer() {
  return (
    <footer className="footer">
      <div className="container stack">
        <span className="wordmark">
          <Wordmark />
        </span>
        <span>Garmin 連携は、いまはダミーデータで動作しています。</span>
      </div>
    </footer>
  )
}
