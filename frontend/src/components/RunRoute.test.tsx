import { render, screen } from '@testing-library/react'
import L from 'leaflet'
import { RunRoute } from './RunRoute'

const route = [
  { latitude: 0.0001, longitude: 0.0001, lap: 1 },
  { latitude: 0.0005, longitude: 0.0003, lap: 1 },
  { latitude: 0.001, longitude: 0.0004, lap: 2 },
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

  it('highlights the chosen lap and lets go of it', () => {
    const { container, rerender } = render(<RunRoute route={route} highlightLap={null} />)
    expect(container.querySelector('.route-highlight')).toBeNull()

    rerender(<RunRoute route={route} highlightLap={2} />)
    expect(container.querySelectorAll('.route-highlight')).toHaveLength(1)
    expect(screen.getByText('ラップ 2 を表示中')).toBeInTheDocument()

    rerender(<RunRoute route={route} highlightLap={null} />)
    expect(container.querySelector('.route-highlight')).toBeNull()
    expect(screen.queryByText(/を表示中/)).not.toBeInTheDocument()
  })

  it('keeps the map still while laps are picked one after another, as long as the lap is in view', () => {
    const inView = vi.spyOn(L.LatLngBounds.prototype, 'contains').mockReturnValue(true)
    const fitBounds = vi.spyOn(L.Map.prototype, 'fitBounds')
    const { rerender } = render(<RunRoute route={route} highlightLap={null} />)
    fitBounds.mockClear()

    rerender(<RunRoute route={route} highlightLap={1} />)
    rerender(<RunRoute route={route} highlightLap={2} />)
    rerender(<RunRoute route={route} highlightLap={null} />)

    expect(fitBounds).not.toHaveBeenCalled()
    inView.mockRestore()
    fitBounds.mockRestore()
  })

  it('moves to a picked lap that is out of view, without first going back to the whole course', () => {
    const inView = vi.spyOn(L.LatLngBounds.prototype, 'contains').mockReturnValue(false)
    const fitBounds = vi.spyOn(L.Map.prototype, 'fitBounds')
    const { rerender } = render(<RunRoute route={route} highlightLap={1} />)
    fitBounds.mockClear()

    rerender(<RunRoute route={route} highlightLap={2} />)

    expect(fitBounds).toHaveBeenCalledTimes(1)
    const [bounds] = fitBounds.mock.calls[0]!
    expect((bounds as L.LatLngBounds).getSouth()).toBeCloseTo(0.0005)
    inView.mockRestore()
    fitBounds.mockRestore()
  })

  it('goes away cleanly while a lap is highlighted', () => {
    const { unmount } = render(<RunRoute route={route} highlightLap={2} />)

    expect(() => unmount()).not.toThrow()
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
