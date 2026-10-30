import type { QueryClient } from '@tanstack/react-query'
import { createRootRouteWithContext, Outlet } from '@tanstack/react-router'
import { Footer } from '../components/Footer'
import { TopNav } from '../components/TopNav'

export const Route = createRootRouteWithContext<{ queryClient: QueryClient }>()({
  component: () => (
    <>
      <TopNav />
      <main>
        <Outlet />
      </main>
      <Footer />
    </>
  ),
  notFoundComponent: () => (
    <section className="band">
      <div className="container stack">
        <h1 className="display-md">ページが見つかりません</h1>
      </div>
    </section>
  ),
})
