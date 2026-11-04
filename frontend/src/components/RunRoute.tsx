import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { useEffect, useRef } from 'react'
import type { RoutePoint } from '../api/client'
import { lapSegment } from '../lib/route'

const TILES = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png'
const ATTRIBUTION = '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'

/**
 * A run's course on a map (ADR-0013). Without positions (a treadmill run or a manual entry) there is
 * no map, so no tile is requested and nothing about where the runner was leaves the browser.
 */
type Props = {
  route: RoutePoint[]
  /** The lap to highlight, picked in the lap table; null or absent for the whole course. */
  highlightLap?: number | null
}

export function RunRoute({ route, highlightLap = null }: Props) {
  return (
    <section className="stack run-route" aria-label="コース" style={{ gap: 'var(--spacing-sm)' }}>
      <h3 className="caption-uppercase muted">コース</h3>
      {route.length === 0 ? (
        <p className="body-sm muted" style={{ margin: 0 }}>
          位置情報なし
        </p>
      ) : (
        <>
          <RouteMap route={route} highlightLap={highlightLap} />
          {highlightLap !== null && (
            <p className="caption muted" role="status" style={{ margin: 0 }}>
              ラップ {highlightLap} を表示中
            </p>
          )}
        </>
      )}
    </section>
  )
}

function RouteMap({ route, highlightLap }: { route: RoutePoint[]; highlightLap: number | null }) {
  const element = useRef<HTMLDivElement>(null)
  const map = useRef<L.Map | null>(null)

  useEffect(() => {
    const created = L.map(element.current!, { scrollWheelZoom: false })
    L.tileLayer(TILES, { maxZoom: 19, attribution: ATTRIBUTION }).addTo(created)

    const points = route.map(toLatLng)
    // Styled in global.css with the DESIGN.md tokens: SVG attributes cannot hold var(--...).
    L.polyline(points, { className: 'route-line', interactive: false }).addTo(created)
    L.circleMarker(points[0]!, { className: 'route-start', radius: 7, interactive: false }).addTo(created)
    L.circleMarker(points[points.length - 1]!, { className: 'route-finish', radius: 7, interactive: false }).addTo(created)
    created.fitBounds(L.latLngBounds(points), { padding: [24, 24] })
    map.current = created

    return () => {
      map.current = null
      created.remove()
    }
  }, [route])

  // The highlight is drawn over the course and removed again, without rebuilding the map.
  useEffect(() => {
    const current = map.current
    const segment = highlightLap === null ? [] : lapSegment(route, highlightLap).map(toLatLng)
    if (!current || segment.length < 2) {
      return
    }
    const line = L.polyline(segment, { className: 'route-highlight', interactive: false }).addTo(current)
    current.fitBounds(L.latLngBounds(segment), { padding: [32, 32], maxZoom: 17 })
    return () => {
      line.remove()
      // On unmount the map's own cleanup has already removed it (cleanups run in order); only refit a live map.
      if (map.current === current) {
        current.fitBounds(L.latLngBounds(route.map(toLatLng)), { padding: [24, 24] })
      }
    }
  }, [route, highlightLap])

  return (
    <div
      ref={element}
      className={highlightLap === null ? 'route-map' : 'route-map has-highlight'}
      role="img"
      aria-label="走ったコースの地図"
    />
  )
}

function toLatLng(point: RoutePoint): L.LatLngTuple {
  return [point.latitude, point.longitude]
}
