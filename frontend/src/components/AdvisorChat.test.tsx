import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ComponentProps } from 'react'
import type { ProposedPlan } from '../api/client'
import { AdvisorChat, type ChatEntry } from './AdvisorChat'

const PROVIDERS = [
  { id: 'gemini', displayName: 'Gemini', models: ['gemini-3.5-flash', 'gemini-3.1-pro-preview'] },
  { id: 'fake', displayName: 'Fake', models: ['rule-based'] },
]

const PROPOSAL: ProposedPlan = {
  month: '2026-11',
  targetKm: 150,
  workouts: [
    { date: '2026-11-03', type: 'EASY', distanceKm: 6, note: '' },
    { date: '2026-11-08', type: 'LONG', distanceKm: 20, note: 'LSD' },
  ],
}

function renderChat(overrides: Partial<ComponentProps<typeof AdvisorChat>> = {}) {
  const props: ComponentProps<typeof AdvisorChat> = {
    month: '2026-11',
    providers: PROVIDERS,
    provider: 'gemini',
    model: 'gemini-3.5-flash',
    onChoose: vi.fn(),
    entries: [],
    pending: false,
    onSend: vi.fn(),
    onAdopt: vi.fn(),
    onReset: vi.fn(),
    ...overrides,
  }
  render(<AdvisorChat {...props} />)
  return props
}

describe('AdvisorChat', () => {
  it('sends the typed question', async () => {
    const user = userEvent.setup()
    const props = renderChat()

    await user.type(screen.getByLabelText('相談内容'), '  来月どれくらい走ればいい？ ')
    await user.click(screen.getByRole('button', { name: '送信' }))

    expect(props.onSend).toHaveBeenCalledWith('来月どれくらい走ればいい？')
    expect(screen.getByLabelText('相談内容')).toHaveValue('')
  })

  it('sends a suggestion with one click', async () => {
    const user = userEvent.setup()
    const props = renderChat()

    await user.click(screen.getByRole('button', { name: 'プランを作って' }))

    expect(props.onSend).toHaveBeenCalledWith('プランを作って')
  })

  it('does not send while waiting for an answer', async () => {
    const user = userEvent.setup()
    const props = renderChat({ pending: true })

    await user.click(screen.getByRole('button', { name: 'プランを作って' }))

    expect(props.onSend).not.toHaveBeenCalled()
    expect(screen.getByRole('status')).toHaveTextContent('考えています')
  })

  it('switching provider picks its first model', async () => {
    const user = userEvent.setup()
    const props = renderChat()

    await user.selectOptions(screen.getByLabelText('プロバイダ'), 'fake')

    expect(props.onChoose).toHaveBeenCalledWith('fake', 'rule-based')
  })

  it('offers only the chosen provider’s models', () => {
    renderChat()

    const options = within(screen.getByLabelText('モデル')).getAllByRole('option').map((o) => o.textContent)
    expect(options).toEqual(['gemini-3.5-flash', 'gemini-3.1-pro-preview'])
  })

  it('labels each answer with the provider and model that gave it', () => {
    const entries: ChatEntry[] = [
      { speaker: 'USER', text: '振り返って' },
      { speaker: 'ADVISOR', text: 'よく走れています', provider: 'fake', model: 'rule-based', proposal: null },
    ]
    renderChat({ entries })

    expect(screen.getByText('Fake · rule-based')).toBeInTheDocument()
    expect(screen.queryByLabelText('プラン案')).not.toBeInTheDocument()
  })

  it('shows a proposal and adopts it', async () => {
    const user = userEvent.setup()
    const entries: ChatEntry[] = [
      { speaker: 'USER', text: 'プランを作って' },
      { speaker: 'ADVISOR', text: '提案します', provider: 'gemini', model: 'gemini-3.5-flash', proposal: PROPOSAL },
    ]
    const props = renderChat({ entries })

    const card = screen.getByLabelText('プラン案')
    expect(within(card).getByText('目標 150.0 km · 予定 2 回 / 26.0 km')).toBeInTheDocument()
    expect(within(card).getByText('ロング走')).toBeInTheDocument()

    await user.click(within(card).getByRole('button', { name: 'このプランを採用' }))

    expect(props.onAdopt).toHaveBeenCalledWith(1)
  })

  it('cannot adopt the same proposal twice', () => {
    const entries: ChatEntry[] = [
      { speaker: 'USER', text: 'プランを作って' },
      { speaker: 'ADVISOR', text: '提案します', provider: 'gemini', model: 'gemini-3.5-flash', proposal: PROPOSAL },
    ]
    renderChat({ entries, adoption: { index: 1, state: 'saved' } })

    expect(screen.getByRole('button', { name: '採用しました' })).toBeDisabled()
  })

  it('tells the user their data goes to the provider', () => {
    renderChat()

    expect(screen.getByText(/選択したプロバイダに送信されます/)).toBeInTheDocument()
  })
})

describe('AdvisorChat proposal', () => {
  it('shows the first seven workouts until expanded', async () => {
    const user = userEvent.setup()
    const workouts = Array.from({ length: 10 }, (_, i) => ({
      date: `2026-11-${String(i + 1).padStart(2, '0')}`,
      type: 'EASY' as const,
      distanceKm: 5,
      note: '',
    }))
    const entries: ChatEntry[] = [
      { speaker: 'USER', text: 'プランを作って' },
      { speaker: 'ADVISOR', text: '提案', provider: 'gemini', model: 'gemini-3.5-flash', proposal: { ...PROPOSAL, workouts } },
    ]
    renderChat({ entries })
    const card = screen.getByLabelText('プラン案')

    expect(within(card).getAllByRole('row')).toHaveLength(1 + 7)

    await user.click(within(card).getByRole('button', { name: 'すべて表示（10 件）' }))

    expect(within(card).getAllByRole('row')).toHaveLength(1 + 10)
  })
})
