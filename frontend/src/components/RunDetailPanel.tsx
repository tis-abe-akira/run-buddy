import { useState } from 'react'
import type { RunDetail } from '../api/client'
import { RunLaps } from './RunLaps'
import { RunRoute } from './RunRoute'

/**
 * A run's course and laps together: picking a lap in the table highlights it on the map. The picked lap
 * is the only state; the detail itself comes in through props, so this is tested without a network.
 */
export function RunDetailPanel({ detail }: { detail: RunDetail }) {
  const [selectedLap, setSelectedLap] = useState<number | null>(null)
  const canPick = detail.route.length > 0

  return (
    <>
      <RunRoute route={detail.route} highlightLap={selectedLap} />
      <RunLaps
        state={{ status: 'ready', detail }}
        selectedLap={canPick ? selectedLap : undefined}
        onSelectLap={canPick ? setSelectedLap : undefined}
      />
    </>
  )
}
