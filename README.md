# run-buddy

Garminのランニング記録（Result）と毎月のトレーニング計画（Plan）を一緒に管理し、
計画づくりをAIに相談できるようにするアプリです。

## リポジトリ構成

```
run-buddy/
├── backend/    Spring Boot 4 / Java 25 / Maven の REST API
├── frontend/   Vite + React + TanStack Router のダッシュボード・Plan入力
├── docs/
│   ├── concept.md   コンセプト・ドメイン用語・ロードマップ
│   ├── adr/         Architecture Decision Records
│   ├── guides/      手順書（DB、Tailscale、OpenWiki）
│   └── ci/          オーナーが .github/workflows/ に置くワークフロー
└── openwiki/   OpenWiki が生成する Wiki（INSTRUCTIONS.md だけ人が書く）
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

### 2. 設定ファイル `.env` を作る（任意）

ローカルの設定と秘密情報は、リポジトリのルートの `.env` に書きます。ひな形をコピーして、必要な行だけ埋めます。`.env` は Git に入りません。

```bash
cp .env.example .env
```

| 設定 | 内容 |
|---|---|
| `GEMINI_API_KEY` | AI 相談で Gemini を使うときの API キー（空なら開発用の Fake だけが使える） |
| `RUNBUDDY_ACTIVITY_SOURCE` | ランの記録をどこから取り込むか。`dummy`（既定、生成したデータ）か `garmin` |
| `GARMIN_API_KEY` | Garmin Connect Personal API のキー（`RUNBUDDY_ACTIVITY_SOURCE=garmin` のとき必須） |
| `RUNBUDDY_LOG_LEVEL` | backend のログの詳しさ。`INFO`（既定。取り込み元、取り込みごとの件数、AI の回答）か `DEBUG`（Garmin へのページごとのリクエストなども出る） |
| `RUNBUDDY_BIND_ADDRESS` | backend が待ち受けるアドレス。既定は `127.0.0.1`（このマシンの中からだけ）。ログイン機能がないので変えないでください |
| `RUNBUDDY_DB_PORT` | PostgreSQL のポート（既定 5432。別の PostgreSQL と重なるときに変える） |

- `.env` は Docker Compose と backend の両方が読みます。backend は Spring Boot の `spring.config.import` で読み込みます。
- 値は `KEY=value` の形で書きます。引用符（`"`）や `export` は付けません。
- 本物の環境変数を設定すると、`.env` より優先されます。

### 3. PostgreSQL を起動する

リポジトリの**ルート**（`compose.yaml` がある場所）で実行します。`--wait` を付けると、PostgreSQL が接続を受け付けられるようになるまで待ってから戻ります。

```bash
docker compose up -d --wait
```

`Container run-buddy-postgres-1 Healthy` と出れば準備完了です。初回の起動時に、アプリ用のロール `runbuddy_app` が作られます（`backend/db/init`）。テーブルは backend の起動時に Flyway が作ります。

### 4. backend を起動する（ターミナル 1）

```bash
cd backend
./mvnw spring-boot:run
```

`Started RunBuddyApplication` と出たら起動完了です。別のターミナルで確認できます。

```bash
curl http://localhost:8080/health
```

### 5. frontend を起動する（ターミナル 2）

```bash
cd frontend
npm install
npm run dev
```

http://localhost:5173 を開きます。

### 6. 画面で一通り試す

1. **ダッシュボード:** 「Garminから取り込む」を押す（既定はダミーデータ。本物の Garmin は下の「Garmin から取り込む」を参照）。今月のランが取り込まれ、グラフと記録が表示されます。前の月（‹）に移動して、その月も取り込めます。
2. **プラン:** 上部の「プラン」で、月間目標と予定ワークアウトを入力して保存します。ダッシュボードに計画の線と達成率が出ます。
3. **AI相談:** 上部の「AI相談」で、プロバイダに「Fake（開発用）」を選び、「先月の走りを振り返って」→「プランを作って」と送ります。プラン案の「このプランを採用」で、その月のプランとして保存されます。

### Garmin から取り込む（任意）

自分の Garmin Connect の記録を取り込めます（[ADR-0009](docs/adr/0009-garmin-personal-api.md)）。

1. Garmin Connect Web にログインした状態で「Personal API Key」ページを開き、「Create Key」でキーを作ります（利用規約への同意と、有効期限（30日〜1年）の選択があります）。キーは一度しか表示されないので、パスワードと同じように保管してください。
2. `.env` に次の2行を書き、backend を起動し直します。

   ```
   RUNBUDDY_ACTIVITY_SOURCE=garmin
   GARMIN_API_KEY=作ったキー
   ```

3. ダッシュボードの「Garminから取り込む」で、その月のランニング（トレイルランなどのサブ種別を含む）が取り込まれます。「ダミーデータ」のバッジは表示されなくなります。

- 取り込むのは、日時（走った場所の現地時刻）・距離・タイム・平均心拍だけです。位置情報や名前は読みません。0 m の手入力の記録は取り込みません。
- `RUNBUDDY_ACTIVITY_SOURCE=garmin` でキーが空だと、backend は起動時にエラーで止まります。
- キーの期限が切れると、取り込みのときに「Garmin が API キーを受け付けませんでした」と表示されます。新しいキーを作って `.env` を書き換えてください。
- ダミーで取り込んだ月を Garmin で取り込み直すと、両方が残って距離が二重になります。Garmin に切り替えるときは、`docker compose down -v` で DB を作り直すのが簡単です。
- AI 相談で Gemini を使うと、取り込んだランの記録が Google に送られます。Garmin の Personal API の利用規約でこれが許されているかは未確認です（ADR-0009）。

### Gemini で相談する（任意）

[Google AI Studio](https://aistudio.google.com/) で API キーを取得し、`.env` の `GEMINI_API_KEY=` に書いて backend を起動し直します。画面のプロバイダに「Gemini」が出て、既定になります。

- モデルの候補は `gemini-2.5-flash`（既定、速い）と `gemini-3.5-flash`（遅いが分析が詳しい）です。
- 相談内容とランの記録は Google に送信されます。利用料がかかることがあります。
- 無料枠には1分あたりの利用上限があり、AI がツールを呼ぶたびに1回分を使います。「利用上限に達しました」と出たら、1分ほど待ってから送り直してください。

### スマホや別の端末から使う（任意）

いつも起動しているマシン（Mac mini など）で動かし、Tailscale でつなぐと、スマホや外出先からも使えます。インターネットには公開しません。手順は [docs/guides/tailscale.md](docs/guides/tailscale.md) にあります（[ADR-0010](docs/adr/0010-run-at-home-over-tailscale.md)）。

画面と API を1つにまとめて起動するには、`-Papp` を付けます（frontend もビルドされます。Node.js が必要です）。

```bash
cd backend && ./mvnw spring-boot:run -Papp
```

http://localhost:8080 で画面が開きます。

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

- **`port 5432 is already allocated`、または `docker compose ps` の PORTS が `0.0.0.0:5432->5432/tcp` ではなく `5432/tcp` だけになっている:** Mac の 5432 番を別のプログラムが使っています（`lsof -nP -iTCP:5432 -sTCP:LISTEN` で確認できます）。`.env` に `RUNBUDDY_DB_PORT=5433` と書き、PostgreSQL を作り直してから backend を起動し直します（Compose と backend の両方が `.env` を読むので、どちらにも反映されます）。

  ```bash
  docker compose down && docker compose up -d --wait
  ```

- **Garmin を選んだのにダミーのデータが取り込まれる:** backend の起動ログの `Activity source:` の行を見てください。`dummy` と出ていれば、`RUNBUDDY_ACTIVITY_SOURCE=garmin` が読まれていません（`.env` の書き間違い、同じキーが2行ある、シェルに同じ名前の環境変数がある、など）。`curl http://localhost:8080/api/activity-source` でも確かめられます。取り込むたびに `Import 2026-10: 29 runs from GarminActivitySource, ...` のようなログが出るので、どこから取ったかも分かります。garmin なのにダミーのランが見える場合は、以前に取り込んだダミーのデータが DB に残っています。`docker compose down -v` で作り直すか、[ダミーのランだけを消す](docs/guides/database.md#ダミーのランだけを消す)手順で消します。
- **`.env` に書いた値が効かない:** 同じ名前の環境変数がシェルに設定されていると、そちらが優先されます（`env | grep GEMINI` で確認できます）。また、値を引用符で囲むと、引用符まで値に含まれてしまいます。
- **`role "runbuddy_app" does not exist`:** 古い DB のデータが残っていて、初期化スクリプトが流れていません。`docker compose down -v` してから起動し直します。
- **DB の中身を見たい:** [データベースガイド](docs/guides/database.md#中身を見る)を参照してください（`psql` を手元に入れなくても見られます）。
- **`release version 25 not supported`:** JDK 25 で動いていません。`JAVA_HOME` を JDK 25 に向けます。
- **Docker を使わずに試したい:** backend を `memory` プロファイルで起動します。DB がなくても動きますが、データは再起動で消えます。

  ```bash
  cd backend
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=memory
  ```

## backend（開発者向けの詳細）

永続化は PostgreSQL + jOOQ で、データは行レベルセキュリティ（RLS）でユーザーごとに分けています（[ADR-0007](docs/adr/0007-persistence-library.md)）。
DB の中身の見方、RLS の仕組み、スキーマの変え方は **[データベースガイド](docs/guides/database.md)** にまとめています。

### AI 相談（Phase 2）

画面の「AI相談」から、プロバイダとモデルを選んで相談できます（[ADR-0008](docs/adr/0008-ai-integration.md)）。

- **Gemini:** `GEMINI_API_KEY` を設定すると選べるようになります。モデルの候補は `runbuddy.ai.gemini.models` で設定します。
- **Fake（開発用）:** API キーがなくても使えるルールベースの相談相手です（`runbuddy.ai.fake.enabled=true`）。本番では無効にしてください。
- **実際の Gemini を呼ぶテスト:** `GEMINI_API_KEY` があるときだけ動きます。

本物の Gemini を呼ぶテストは、`.env` に API キーがあっても普段の `./mvnw test` では動きません（毎回 API を呼んで、利用料や利用上限を消費しないようにするため）。動かすときは、環境変数でキーを渡します。

```bash
cd backend
GEMINI_API_KEY=$(grep '^GEMINI_API_KEY=' ../.env | cut -d= -f2-) ./mvnw test -Dtest=GeminiLiveTest
```

### Garmin 連携（Phase 4）

`RUNBUDDY_ACTIVITY_SOURCE=garmin` で、`activity/garmin` の `GarminActivitySource` が使われます（[ADR-0009](docs/adr/0009-garmin-personal-api.md)）。本物の Garmin を呼ぶテスト `GarminLiveTest` は、OS の環境変数 `GARMIN_API_KEY` があるときだけ動きます（読み取りだけで、料金はかかりません）。

```bash
cd backend
GARMIN_API_KEY=$(grep '^GARMIN_API_KEY=' ../.env | cut -d= -f2-) ./mvnw test -Dtest=GarminLiveTest
```

### API

起動後は `GET http://localhost:8080/health` が `{"status":"ok"}` を返します。

| API | 内容 |
|---|---|
| `GET /api/dashboard/{YYYY-MM}` | 計画と実績の比較（達成率、日ごとの累積など） |
| `GET /api/runs/{YYYY-MM}` | ランの記録 |
| `POST /api/runs/{YYYY-MM}/import` | ActivitySource（ダミーか Garmin）から取り込む。何度呼んでも重複しない。失敗すると ProblemDetail の `reason` に `CREDENTIALS_REJECTED` / `RATE_LIMITED` / `UNAVAILABLE` |
| `GET /api/activity-source` | 今の取り込み元（`dummy` / `garmin`） |
| `GET` / `PUT /api/plans/{YYYY-MM}` | 月次プラン |
| `GET /api/export/{YYYY-MM}.csv` | その月のプランと実績を日別の CSV でダウンロードする（1日1行＋合計行。UTF-8 BOM 付きなので Excel でもそのまま開ける）。ダッシュボードの「CSV でエクスポート」ボタンから使う |
| `GET /api/advisor/providers` | 使える AI プロバイダとモデルの一覧 |
| `POST /api/advisor/consultations` | AI に相談する（会話の履歴を毎回送る。プラン案は保存しない） |
| `GET /v3/api-docs` | OpenAPI 定義（frontend の型生成に使う） |


## frontend（開発者向けの詳細）

[frontend/README.md](frontend/README.md) を参照してください。

## AI エージェント向けのルール

- [backend/CLAUDE.md](backend/CLAUDE.md)
- [frontend/CLAUDE.md](frontend/CLAUDE.md)

## ドキュメント

- [コンセプト](docs/concept.md)
- [ADR 一覧](docs/adr/README.md)
- [データベースガイド](docs/guides/database.md)（中身の見方、RLS、スキーマの変え方）
- [スマホや別の端末から使う（Tailscale）](docs/guides/tailscale.md)
- [Wiki の自動生成（OpenWiki）](docs/guides/openwiki.md)（アーキテクチャの解説とチュートリアル。公開先: https://tis-abe-akira.github.io/run-buddy/ ）
