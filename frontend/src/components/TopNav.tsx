import { Link } from '@tanstack/react-router'
import { currentMonth } from '../lib/month'
import { Wordmark } from './Wordmark'

export function TopNav() {
  const month = currentMonth()
  return (
    <header className="top-nav">
      <div className="container">
        <Link to="/dashboard" search={{}} className="wordmark">
          <Wordmark />
        </Link>
        <nav className="nav-links" aria-label="メイン">
          <Link to="/dashboard" search={{}} className="nav-link">
            ダッシュボード
          </Link>
          <Link to="/plans/$month" params={{ month }} className="nav-link">
            プラン
          </Link>
        </nav>
      </div>
    </header>
  )
}
