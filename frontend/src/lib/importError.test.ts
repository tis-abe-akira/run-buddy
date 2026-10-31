import { ApiError } from '../api/client'
import { describeImportError } from './importError'

describe('describeImportError', () => {
  it('tells the user to renew a rejected key', () => {
    const message = describeImportError(new ApiError(502, 'Garmin rejected the Personal API key', 'CREDENTIALS_REJECTED'))
    expect(message).toContain('新しいキー')
    expect(message).toContain('GARMIN_API_KEY')
  })

  it('asks to wait on the rate limit', () => {
    expect(describeImportError(new ApiError(429, 'rate limit', 'RATE_LIMITED'))).toContain('しばらく待って')
  })

  it('shows the detail of an outage', () => {
    expect(describeImportError(new ApiError(502, 'Garmin answered HTTP 500', 'UNAVAILABLE'))).toContain('Garmin answered HTTP 500')
  })

  it('falls back to the message for anything else', () => {
    expect(describeImportError(new ApiError(500, 'boom'))).toBe('取り込めませんでした: boom')
    expect(describeImportError(new Error('Failed to fetch'))).toBe('取り込めませんでした: Failed to fetch')
  })
})
