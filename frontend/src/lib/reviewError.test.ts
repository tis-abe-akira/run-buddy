import { ApiError } from '../api/client'
import { describeReviewError } from './reviewError'

describe('describeReviewError', () => {
  it('asks to wait when the AI is rate limited', () => {
    expect(describeReviewError(new ApiError(429, 'gemini rate limit or quota exceeded'))).toBe(
      'AI の利用上限に達しました。1分ほど待ってから、もう一度評価してもらってください。',
    )
  })

  it('passes on why the AI could not review the run', () => {
    expect(describeReviewError(new ApiError(502, 'gemini could not review the run: timeout'))).toBe(
      'AI が評価できませんでした（gemini could not review the run: timeout）。時間をおいて、もう一度試してください。',
    )
  })

  it('says the AI took too long', () => {
    expect(describeReviewError(new ApiError(504, 'gemini did not answer within 60 s'))).toBe(
      'AI の応答に時間がかかりすぎました。少し待ってから、もう一度評価してもらってください。',
    )
  })

  it('says the run is gone', () => {
    expect(describeReviewError(new ApiError(404, 'no run x'))).toBe('このランが見つかりません。取り込み直してから開いてください。')
  })

  it('falls back to the error itself', () => {
    expect(describeReviewError(new Error('network down'))).toBe('評価できませんでした: network down')
  })
})
