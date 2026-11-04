import type { RoutePoint } from '../api/client'

/**
 * The part of the route run during one lap, for highlighting it on the map. It starts at the last
 * point of the previous lap, so the highlighted line meets the rest of the course without a gap.
 */
export function lapSegment(route: RoutePoint[], lap: number): RoutePoint[] {
  const first = route.findIndex((point) => point.lap === lap)
  if (first === -1) {
    return []
  }
  let end = first
  while (end + 1 < route.length && route[end + 1]!.lap === lap) {
    end++
  }
  return route.slice(Math.max(first - 1, 0), end + 1)
}
