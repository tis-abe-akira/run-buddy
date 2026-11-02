import { render, screen } from '@testing-library/react'
import { aRace } from '../test/races'
import { NextRaceCard } from './NextRaceCard'

describe('NextRaceCard', () => {
  it('counts down to the next race and shows the goal', () => {
    render(<NextRaceCard race={aRace()} />)

    const card = screen.getByRole('region', { name: '次のレース' })
    expect(card).toHaveTextContent('Autumn marathon')
    expect(card).toHaveTextContent('11/22（日）')
    expect(card).toHaveTextContent('あと 21 日')
    expect(card).toHaveTextContent('目標 5:00:00 · 7:06/km')
  })

  it('a race to finish says so', () => {
    render(<NextRaceCard race={aRace({ targetSeconds: null, targetPaceSecondsPerKm: null })} />)

    expect(screen.getByRole('region', { name: '次のレース' })).toHaveTextContent('完走')
  })
})
