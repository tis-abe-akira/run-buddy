import type { RoutePoint } from '../api/client'
import { lapSegment } from './route'

const point = (latitude: number, lap: number | null): RoutePoint => ({
  latitude,
  longitude: 0,
  lap,
})

describe('lapSegment', () => {
  const route = [point(1, 1), point(2, 1), point(3, 2), point(4, 2), point(5, 3)]

  it('takes the points of the lap, joined to where the previous lap ended so the line has no gap', () => {
    expect(lapSegment(route, 2).map((p) => p.latitude)).toEqual([2, 3, 4])
  })

  it('starts the first lap at its own first point', () => {
    expect(lapSegment(route, 1).map((p) => p.latitude)).toEqual([1, 2])
  })

  it('takes the last lap through the finish', () => {
    expect(lapSegment(route, 3).map((p) => p.latitude)).toEqual([4, 5])
  })

  it('is empty for a lap that has no position', () => {
    expect(lapSegment(route, 9)).toEqual([])
    expect(lapSegment([point(1, null)], 1)).toEqual([])
  })
})
