import type { WorkoutType } from '../api/client'

export const WORKOUT_TYPES: { value: WorkoutType; label: string }[] = [
  { value: 'EASY', label: 'イージー' },
  { value: 'TEMPO', label: 'テンポ走' },
  { value: 'INTERVAL', label: 'インターバル' },
  { value: 'LONG', label: 'ロング走' },
  { value: 'RACE', label: 'レース' },
]

/** "LONG" -> "ロング走"; an unknown type is shown as it is. */
export function workoutTypeLabel(type: string): string {
  return WORKOUT_TYPES.find((t) => t.value === type)?.label ?? type
}
