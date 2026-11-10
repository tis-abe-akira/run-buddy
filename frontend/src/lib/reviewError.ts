import { ApiError } from '../api/client'

/** What went wrong with an AI review of a run (#35), and what to do about it, in the user's words. */
export function describeReviewError(error: Error): string {
  if (error instanceof ApiError) {
    switch (error.status) {
      case 429:
        return 'AI の利用上限に達しました。1分ほど待ってから、もう一度評価してもらってください。'
      case 502:
        return `AI が評価できませんでした（${error.detail ?? error.status}）。時間をおいて、もう一度試してください。`
      case 404:
        return 'このランが見つかりません。取り込み直してから開いてください。'
    }
  }
  return `評価できませんでした: ${error.message}`
}
