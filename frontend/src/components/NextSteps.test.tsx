import { screen, within } from '@testing-library/react'
import type { NextStepCard } from '../api/client'
import { renderWithRouter } from '../test/router'
import { NextSteps } from './NextSteps'

const behind: NextStepCard = { kind: 'BEHIND_TARGET', month: '2026-11', shortfallKm: 30, remainingKm: 80, daysLeft: 10 }
const monthEnding: NextStepCard = { kind: 'MONTH_ENDING', nextMonth: '2026-12', daysLeft: 5 }

describe('NextSteps', () => {
  it('shows the most pressing card first, as the callout', async () => {
    await renderWithRouter(<NextSteps cards={[behind, monthEnding]} thisMonth="2026-11" />)

    const list = screen.getByRole('region', { name: 'いまやること' })
    const [first, second] = within(list).getAllByRole('article')
    expect(first).toHaveClass('callout-coral')
    expect(within(first!).getByRole('heading')).toHaveTextContent('目標まであと 80.0 km')
    expect(second).not.toHaveClass('callout-coral')
    expect(within(second!).getByRole('heading')).toHaveTextContent('2026年12月の計画がまだです')
  })

  it('opens the advisor with the request in the message box, without sending it', async () => {
    await renderWithRouter(<NextSteps cards={[behind]} thisMonth="2026-11" />)

    const link = screen.getByRole('link', { name: '計画を見直す' })
    const url = new URL(link.getAttribute('href')!, 'http://localhost')
    expect(url.pathname).toBe('/advisor')
    expect(url.searchParams.get('month')).toBe('2026-11')
    expect(url.searchParams.get('ask')).toContain('あと 80.0 km')
  })

  it('links to next month’s plan', async () => {
    await renderWithRouter(<NextSteps cards={[monthEnding]} thisMonth="2026-11" />)

    expect(screen.getByRole('link', { name: '計画を作る' })).toHaveAttribute('href', '/plans/2026-12')
  })

  it('shows nothing when there is nothing to do', async () => {
    const { container } = await renderWithRouter(<NextSteps cards={[]} thisMonth="2026-11" />)

    expect(screen.queryByRole('region', { name: 'いまやること' })).not.toBeInTheDocument()
    expect(container.querySelector('article')).toBeNull()
  })
})
