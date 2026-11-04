import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { useEffect, useRef } from 'react'
import type { RoutePoint } from '../api/client'

const TILES = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png'
const ATTRIBUTION = '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'

/**
 * A run's course on a map (ADR-0013). Without positions (a treadmill run or a manual entry) there is
 * no map, so no tile is requested and nothing about where the runner was leaves the browser.
 */
export function RunRoute({ route }: { route: RoutePoint[] }) {
  return (
    <section className="stack run-route" aria-label="コース" style={{ gap: 'var(--spacing-sm)' }}>
      <h3 className="caption-uppercase muted">コース</h3>
      {route.length === 0 ? (
        <p className="body-sm muted" style={{ margin: 0 }}>
          位置情報なし
        </p>
      ) : (
        <RouteMap route={route} />
      )}
    </section>
  )
}

function RouteMap({ route }: { route: RoutePoint[] }) {
  const element = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const map = L.map(element.current!, { scrollWheelZoom: false })
    L.tileLayer(TILES, { maxZoom: 19, attribution: ATTRIBUTION }).addTo(map)

    const points = route.map((point): L.LatLngTuple => [point.latitude, point.longitude])
    // Styled in global.css with the DESIGN.md tokens: SVG attributes cannot hold var(--...).
    L.polyline(points, { className: 'route-line', interactive: false }).addTo(map)
    L.circleMarker(points[0]!, { className: 'route-start', radius: 7, interactive: false }).addTo(map)
    L.circleMarker(points[points.length - 1]!, { className: 'route-finish', radius: 7, interactive: false }).addTo(map)
    map.fitBounds(L.latLngBounds(points), { padding: [24, 24] })

    return () => {
      map.remove()
    }
  }, [route])

  return <div ref={element} className="route-map" role="img" aria-label="走ったコースの地図" />
}
