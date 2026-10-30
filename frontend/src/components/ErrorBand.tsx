export function ErrorBand({ error }: { error: unknown }) {
  return (
    <section className="band">
      <div className="container stack">
        <h1 className="display-md">データを読み込めませんでした</h1>
        <p className="muted" style={{ margin: 0 }}>
          バックエンド（http://localhost:8080）が起動しているか確認してください。
        </p>
        <p className="form-error" style={{ margin: 0 }}>
          {error instanceof Error ? error.message : String(error)}
        </p>
      </div>
    </section>
  )
}
