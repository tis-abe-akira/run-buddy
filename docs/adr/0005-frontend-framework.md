# ADR-0005: frontend に Vite + TanStack Router（SPA）を使う

- ステータス: Accepted
- 日付: 2026-10-30

## コンテキスト

frontend の役割は、ダッシュボード（計画と実績の可視化）と Plan の入力フォームです。

- 業務ロジックと外部連携（Garmin / LLM / スプレッドシート）はすべて Spring Boot の backend に置く（ADR-0002, ADR-0004）。
- ログインした本人だけが使うアプリなので、SEO や SSR は必要ない。
- 年月やフィルタなど、URL のクエリパラメータで状態を持つ画面が多くなりそう（例: `/dashboard?month=2026-11`）。

## 検討した選択肢

| 観点 | Next.js（App Router） | Vite + TanStack Router（SPA） |
|---|---|---|
| サーバー機能（RSC、Route Handlers、Server Actions） | 豊富。ただし backend が別にあるので、ほとんど使わないか、「BFFにロジックを置くか」で迷う原因になる | 持たない。責務は「UI だけ」とはっきりする |
| ルーティングの型安全性 | パスやパラメータの型は部分的 | パス・パラメータ・**検索パラメータ**まで型が付き、バリデーションもできる |
| データ取得 | `fetch` + キャッシュの仕組みが独特 | TanStack Query と組み合わせるのが定番。ルートの `loader` で先読みもできる |
| デプロイ | Node ランタイムか Vercel が前提になりやすい | 静的ファイルなので、Spring Boot から配信することも、CDN に置くこともできる |
| 情報量・AIの得意さ | とても多い | Next.js よりは少ないが、十分ある。API は安定している |
| テスト | サーバーコンポーネントの単体テストはやや面倒 | 普通の React コンポーネントなので Vitest + Testing Library で素直にテストできる |

## 決定

**Vite + React + TypeScript + TanStack Router（file-based routing）+ TanStack Query** の SPA にする。

- 型の生成: backend の OpenAPI（springdoc）から TypeScript の型を作り、API の契約を両側で守る。
- テスト: Vitest + Testing Library。表示用コンポーネントは props だけを受け取るようにし、ネットワークなしでテストする。ルートとAPIを含めた結合テストが必要になったら MSW を導入する。
- TanStack Start（SSR 版）は、SSR が必要になるまで使わない。

## 結果（トレードオフ）

- 良い点: frontend / backend の責務がはっきりし、Java 側にドメインロジックを集められる。
- 良い点: URL の状態（年月など）まで型安全になり、ダッシュボードとの相性が良い。
- 注意点: Next.js と比べると情報やサンプルが少ない。
- 注意点: 将来、公開ページや SEO が必要になったら見直す（TanStack Start に移るか、Next.js を検討する）。
