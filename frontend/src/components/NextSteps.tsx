import { Link } from '@tanstack/react-router'
import type { NextStepCard } from '../api/client'
import type { Month } from '../lib/month'
import { describeNextStep, type NextStepAction } from '../lib/nextSteps'

type Props = {
  /** From GET /api/next-steps, most pressing first. */
  cards: NextStepCard[]
  /** Today's month. */
  thisMonth: Month
}

/**
 * What is worth doing today (#26, #27). The first card is the coral callout; the rest are quieter.
 * Buttons only open a page: nothing is saved from here.
 */
export function NextSteps({ cards, thisMonth }: Props) {
  if (cards.length === 0) {
    return null
  }
  return (
    <section className="next-steps" aria-label="いまやること">
      {cards.map((card, i) => {
        const step = describeNextStep(card, thisMonth)
        const lead = i === 0
        return (
          <article key={card.kind} className={lead ? 'callout-coral next-step' : 'outline-card next-step'}>
            <h2 className="display-sm numeric">{step.title}</h2>
            <p className="body-sm" style={{ margin: 0 }}>
              {step.body}
            </p>
            {step.actions.length > 0 && (
              <div className="row">
                {step.actions.map((action, j) => (
                  <ActionLink key={action.label} action={action} className={buttonClass(lead, j === 0)} />
                ))}
              </div>
            )}
          </article>
        )
      })}
    </section>
  )
}

function buttonClass(lead: boolean, primary: boolean): string {
  if (lead) {
    return primary ? 'button button-on-coral' : 'text-link body-sm next-step-secondary'
  }
  return primary ? 'button button-secondary' : 'text-link body-sm'
}

function ActionLink({ action, className }: { action: NextStepAction; className: string }) {
  if (action.to === 'plan') {
    return (
      <Link to="/plans/$month" params={{ month: action.month }} className={className}>
        {action.label}
      </Link>
    )
  }
  return (
    <Link to="/advisor" search={{ month: action.month, ask: action.ask }} className={className}>
      {action.label}
    </Link>
  )
}
