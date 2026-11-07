import { createMemoryHistory, createRootRoute, createRouter, Outlet, RouterProvider } from '@tanstack/react-router'
import { render, screen } from '@testing-library/react'
import type { ReactNode } from 'react'

/**
 * Renders a component that uses router `Link`s, inside a router of its own that has just one page.
 * The links get their real `href`, so a test can check where each one goes without the app's routes.
 */
export async function renderWithRouter(ui: ReactNode) {
  const rootRoute = createRootRoute({
    component: () => (
      <>
        <div data-testid="rendered">{ui}</div>
        <Outlet />
      </>
    ),
  })
  const router = createRouter({ routeTree: rootRoute, history: createMemoryHistory({ initialEntries: ['/'] }) })
  const result = render(<RouterProvider router={router} />)
  await screen.findByTestId('rendered')
  return result
}
