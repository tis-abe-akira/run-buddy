import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ExportLink } from './ExportLink'

describe('ExportLink', () => {
  it('links to the CSV of the shown month as a download', () => {
    render(<ExportLink month="2026-11" />)

    const link = screen.getByRole('link', { name: 'CSV でエクスポート' })
    expect(link).toHaveAttribute('href', '/api/export/2026-11.csv')
    expect(link).toHaveAttribute('download')
  })
})
