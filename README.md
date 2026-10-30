# run-buddy

Garminのランニング記録（Result）と毎月のトレーニング計画（Plan）を一緒に管理し、
計画づくりをAIに相談できるようにするアプリです。

## リポジトリ構成

```
run-buddy/
├── backend/    Spring Boot 4 / Java 25 / Maven の REST API
├── frontend/   Vite + React + TanStack Router のダッシュボード・Plan入力
└── docs/
    ├── concept.md   コンセプト・ドメイン用語・ロードマップ
    └── adr/         Architecture Decision Records
```

## backend

前提: JDK 25, Maven 3.9+

```bash
cd backend
mvn test
```

```bash
cd backend
mvn spring-boot:run
```

起動後は `GET http://localhost:8080/health` が `{"status":"ok"}` を返します。

| API | 内容 |
|---|---|
| `GET /api/dashboard/{YYYY-MM}` | 計画と実績の比較（達成率、日ごとの累積など） |
| `GET /api/runs/{YYYY-MM}` | ランの記録 |
| `POST /api/runs/{YYYY-MM}/import` | ActivitySource（Phase 1 はダミー）から取り込む。何度呼んでも重複しない |
| `GET` / `PUT /api/plans/{YYYY-MM}` | 月次プラン |
| `GET /v3/api-docs` | OpenAPI 定義（frontend の型生成に使う） |

Phase 1 のデータはメモリ上にだけあり、再起動で消えます。

## frontend

前提: Node.js 22.12+。backend を起動した状態で:

```bash
cd frontend
npm install
npm run dev
```

http://localhost:5173 を開きます。詳しくは [frontend/README.md](frontend/README.md)。

## ドキュメント

- [コンセプト](docs/concept.md)
- [ADR 一覧](docs/adr/README.md)
