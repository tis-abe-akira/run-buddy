import { ApiError } from '../api/client'

/** What went wrong with an import from Garmin, and what to do about it, in the user's words. */
export function describeImportError(error: Error): string {
  if (error instanceof ApiError) {
    switch (error.reason) {
      case 'CREDENTIALS_REJECTED':
        return 'Garmin が API キーを受け付けませんでした。期限切れか削除された可能性があります。Garmin Connect の「Personal API Key」ページで新しいキーを作り、.env の GARMIN_API_KEY を書き換えて backend を起動し直してください。'
      case 'RATE_LIMITED':
        return 'Garmin の利用上限に達しました。しばらく待ってから、もう一度取り込んでください。'
      case 'UNAVAILABLE':
        return `Garmin から取得できませんでした。ネットワークを確認し、時間をおいて取り込み直してください（${error.detail ?? error.status}）。`
    }
  }
  return `取り込めませんでした: ${error.message}`
}

/** What went wrong while loading a run's laps (ADR-0012): the first look downloads its activity file from Garmin. */
export function describeDetailError(error: Error): string {
  if (error instanceof ApiError) {
    switch (error.reason) {
      case 'UNREADABLE_FILE':
        return 'このランのアクティビティファイルを読み取れませんでした。'
      case 'CREDENTIALS_REJECTED':
      case 'RATE_LIMITED':
      case 'UNAVAILABLE':
        return describeImportError(error).replace('取り込んでください', '開いてください').replace('取り込み直してください', '開き直してください')
    }
  }
  return `ラップを読み込めませんでした: ${error.message}`
}
