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

前提: JDK 25, Maven 3.9+, Docker（テストと開発用の PostgreSQL に使う）

```bash
cd backend
mvn test
```

DB を使うテストは Testcontainers が PostgreSQL 17 を自動で起動します。

開発用の PostgreSQL を起動してから backend を動かします。

```bash
docker compose up -d
```

```bash
cd backend
mvn spring-boot:run
```

DB なしで動かす場合は `memory` プロファイルを使います（データは再起動で消えます）。

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=memory
```

永続化は jOOQ（既定）と Spring Data JDBC を `runbuddy.persistence=jooq|jdbc` で切り替えられます（[ADR-0007](docs/adr/0007-persistence-library.md) のスパイク中）。
マイグレーションを変更したら、jOOQ のコードを生成し直します（Docker が必要です）。

```bash
cd backend
./scripts/generate-jooq.sh
```

### DB アクセスの約束事

- データは行レベルセキュリティ（RLS）でユーザーごとに分けています。アプリのSQLは `user_id` に触れません。
- DB へのアクセスは必ずトランザクションの中で行います。トランザクションの外で行うと、`app.user_id is not set` というエラーになります（`RlsTransactionManager`）。

起動後は `GET http://localhost:8080/health` が `{"status":"ok"}` を返します。

| API | 内容 |
|---|---|
| `GET /api/dashboard/{YYYY-MM}` | 計画と実績の比較（達成率、日ごとの累積など） |
| `GET /api/runs/{YYYY-MM}` | ランの記録 |
| `POST /api/runs/{YYYY-MM}/import` | ActivitySource（Phase 1 はダミー）から取り込む。何度呼んでも重複しない |
| `GET` / `PUT /api/plans/{YYYY-MM}` | 月次プラン |
| `GET /v3/api-docs` | OpenAPI 定義（frontend の型生成に使う） |


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
