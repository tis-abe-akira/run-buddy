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

## ローカルで動かす

### 前提

| ツール | バージョン | 用途 |
|---|---|---|
| JDK | 25 | backend（Maven は同梱の `./mvnw` が自動で取得するので、入れなくてよい） |
| Node.js | 22.12 以上 | frontend |
| Docker（Docker Desktop など） | Compose v2 が使えること | 開発用の PostgreSQL と、DB を使うテスト |

```bash
java -version
```

```bash
node -v
```

```bash
docker compose version
```

### 1. clone する

```bash
git clone https://github.com/tis-abe-akira/run-buddy.git
```

```bash
cd run-buddy
```

### 2. PostgreSQL を起動する

リポジトリの**ルート**（`compose.yaml` がある場所）で実行します。`--wait` を付けると、PostgreSQL が接続を受け付けられるようになるまで待ってから戻ります。

```bash
docker compose up -d --wait
```

`Container run-buddy-postgres-1 Healthy` と出れば準備完了です。初回の起動時に、アプリ用のロール `runbuddy_app` が作られます（`backend/db/init`）。テーブルは backend の起動時に Flyway が作ります。

### 3. backend を起動する（ターミナル 1）

```bash
cd backend
./mvnw spring-boot:run
```

`Started RunBuddyApplication` と出たら起動完了です。別のターミナルで確認できます。

```bash
curl http://localhost:8080/health
```

### 4. frontend を起動する（ターミナル 2）

```bash
cd frontend
npm install
npm run dev
```

http://localhost:5173 を開きます。

### 5. 画面で一通り試す

1. **ダッシュボード:** 「Garminから取り込む」を押す（Phase 1 はダミーデータ）。今月のランが取り込まれ、グラフと記録が表示されます。前の月（‹）に移動して、その月も取り込めます。
2. **プラン:** 上部の「プラン」で、月間目標と予定ワークアウトを入力して保存します。ダッシュボードに計画の線と達成率が出ます。
3. **AI相談:** 上部の「AI相談」で、プロバイダに「Fake（開発用）」を選び、「先月の走りを振り返って」→「プランを作って」と送ります。プラン案の「このプランを採用」で、その月のプランとして保存されます。

### Gemini で相談する（任意）

[Google AI Studio](https://aistudio.google.com/) で API キーを取得し、環境変数に入れて backend を起動し直します。画面のプロバイダに「Gemini」が出ます。

```bash
cd backend
GEMINI_API_KEY=取得したキー ./mvnw spring-boot:run
```

相談内容とランの記録が Google に送信され、利用料がかかることがあります。

### テストを流す

```bash
cd backend
./mvnw test
```

```bash
cd frontend
npm test
```

backend の DB を使うテストは、Testcontainers が PostgreSQL のコンテナを自動で起動します（Docker が動いている必要があります）。

### 片付け

backend と frontend は Ctrl+C で止めます。PostgreSQL のコンテナも止めます。

```bash
docker compose down
```

データも消したいときは `-v` を付けます。

```bash
docker compose down -v
```

### うまく動かないとき

- **`Connection to localhost:5432 refused`（backend の起動時）:** PostgreSQL が動いていません。リポジトリのルートで状態を確認します。

  ```bash
  docker compose ps
  ```

  一覧が空なら `docker compose up -d --wait` を実行します（Docker Desktop が起動していることも確認してください）。`Exited` や `unhealthy` なら、ログで原因を確認します。

  ```bash
  docker compose logs postgres
  ```

- **`port 5432 is already allocated`、または `docker compose ps` の PORTS が `0.0.0.0:5432->5432/tcp` ではなく `5432/tcp` だけになっている:** Mac の 5432 番を別のプログラムが使っています（`lsof -nP -iTCP:5432 -sTCP:LISTEN` で確認できます）。環境変数 `RUNBUDDY_DB_PORT` で別のポートを使えます。PostgreSQL と backend の両方に同じ値を指定してください。

  ```bash
  docker compose down
  ```

  ```bash
  RUNBUDDY_DB_PORT=5433 docker compose up -d --wait
  ```

  ```bash
  cd backend && RUNBUDDY_DB_PORT=5433 ./mvnw spring-boot:run
  ```
- **`role "runbuddy_app" does not exist`:** 古い DB のデータが残っていて、初期化スクリプトが流れていません。`docker compose down -v` してから起動し直します。
- **`release version 25 not supported`:** JDK 25 で動いていません。`JAVA_HOME` を JDK 25 に向けます。
- **Docker を使わずに試したい:** backend を `memory` プロファイルで起動します。DB がなくても動きますが、データは再起動で消えます。

  ```bash
  cd backend
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=memory
  ```

## backend（開発者向けの詳細）

永続化には jOOQ を使います（[ADR-0007](docs/adr/0007-persistence-library.md)）。
マイグレーションを変更したら、jOOQ のコードを生成し直します（Docker が必要です。CI は生成コードに差分があると失敗します）。

```bash
cd backend
./scripts/generate-jooq.sh
```

### AI 相談（Phase 2）

画面の「AI相談」から、プロバイダとモデルを選んで相談できます（[ADR-0008](docs/adr/0008-ai-integration.md)）。

- **Gemini:** `GEMINI_API_KEY` を設定すると選べるようになります。モデルの候補は `runbuddy.ai.gemini.models` で設定します。
- **Fake（開発用）:** API キーがなくても使えるルールベースの相談相手です（`runbuddy.ai.fake.enabled=true`）。本番では無効にしてください。
- **実際の Gemini を呼ぶテスト:** `GEMINI_API_KEY` があるときだけ動きます。

```bash
cd backend
GEMINI_API_KEY=... ./mvnw test -Dtest=GeminiLiveTest
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


## frontend（開発者向けの詳細）

[frontend/README.md](frontend/README.md) を参照してください。

## AI エージェント向けのルール

- [backend/CLAUDE.md](backend/CLAUDE.md)
- [frontend/CLAUDE.md](frontend/CLAUDE.md)

## ドキュメント

- [コンセプト](docs/concept.md)
- [ADR 一覧](docs/adr/README.md)
