import { Wordmark } from './Wordmark'

export function Footer() {
  return (
    <footer className="footer">
      <div className="container stack">
        <span className="wordmark">
          <Wordmark />
        </span>
        <span>ランの記録は Garmin Connect から取り込みます（設定によっては開発用のダミーデータ）。</span>
      </div>
    </footer>
  )
}
