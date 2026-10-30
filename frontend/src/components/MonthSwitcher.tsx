import { formatMonth, shiftMonth, type Month } from '../lib/month'

type Props = {
  month: Month
  onChange: (month: Month) => void
}

export function MonthSwitcher({ month, onChange }: Props) {
  return (
    <div className="row" role="group" aria-label="表示する月">
      <button type="button" className="button-icon" aria-label="前の月" onClick={() => onChange(shiftMonth(month, -1))}>
        ‹
      </button>
      <span className="title-sm numeric" style={{ minWidth: 96, textAlign: 'center' }}>
        {formatMonth(month)}
      </span>
      <button type="button" className="button-icon" aria-label="次の月" onClick={() => onChange(shiftMonth(month, 1))}>
        ›
      </button>
    </div>
  )
}
