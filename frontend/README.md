# frontend

ダッシュボードと月次Planの入力画面です。

- Vite + React + TypeScript
- TanStack Router（ファイルベースルーティング、型付き検索パラメータ）+ TanStack Query
- テスト: Vitest + Testing Library
- デザイン: [DESIGN.md](DESIGN.md)（このアプリ向けの読み替えは [CLAUDE.md](CLAUDE.md) を参照）

## 画面

| パス | 内容 |
|---|---|
| `/dashboard?month=YYYY-MM` | 月の実績・達成率・累積距離グラフ・ランの記録。「Garminから取り込む」（Phase 1 ではダミー） |
| `/plans/YYYY-MM` | 月間目標と予定ワークアウトの入力 |

## 開発

前提: Node.js 22.12 以上。backend を先に起動しておきます（`/api` は Vite が `localhost:8080` に転送します）。

```bash
npm install
```

```bash
npm run dev
```

http://localhost:5173 を開きます。

```bash
npm test
```

```bash
npm run typecheck
```

backend の API を変更したら、backend を起動した状態で型を作り直します。

```bash
npm run gen:api
```
