import { render, screen } from '@testing-library/react'
import { RunRoute } from './RunRoute'

const route = [
  { latitude: 0.0001, longitude: 0.0001 },
  { latitude: 0.0005, longitude: 0.0003 },
  { latitude: 0.001, longitude: 0.0004 },
]

describe('RunRoute', () => {
  it('draws the course on a map and credits OpenStreetMap', () => {
    const { container } = render(<RunRoute route={route} />)

    expect(screen.getByRole('region', { name: 'コース' })).toBeInTheDocument()
    expect(container.querySelector('.leaflet-container')).not.toBeNull()
    expect(screen.getByRole('link', { name: 'OpenStreetMap' })).toHaveAttribute(
      'href',
      'https://www.openstreetmap.org/copyright',
    )
    expect(screen.queryByText('位置情報なし')).not.toBeInTheDocument()
  })

  it('marks the start and the finish', () => {
    const { container } = render(<RunRoute route={route} />)

    expect(container.querySelectorAll('.route-start')).toHaveLength(1)
    expect(container.querySelectorAll('.route-finish')).toHaveLength(1)
    expect(container.querySelectorAll('.route-line')).toHaveLength(1)
  })

  it('says there is no position instead of showing a map when the run has no route', () => {
    const { container } = render(<RunRoute route={[]} />)

    expect(screen.getByText('位置情報なし')).toBeInTheDocument()
    expect(container.querySelector('.leaflet-container')).toBeNull()
  })

  it('removes the map when it goes away', () => {
    const { container, unmount } = render(<RunRoute route={route} />)
    const map = container.querySelector('.leaflet-container')!

    unmount()

    expect(map.querySelector('.leaflet-pane')).toBeNull()
  })
})
