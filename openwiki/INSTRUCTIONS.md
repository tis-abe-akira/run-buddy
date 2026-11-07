# OpenWiki への指示（run-buddy）

このファイルは人間が書く「編集方針」です。OpenWiki はこれを読んで Wiki を書きます。
このファイル以外の `openwiki/` の中身は CI が生成するので、手で直さないでください
（直したいときは、このファイルかソースを直します）。

## 読者と目的

- 読者は、このリポジトリに初めて触る **Java / Spring の開発者**と、ここで作業する **コーディングエージェント**。
- オブジェクト指向設計とソフトウェアテストに関心がある読者を想定する。
  「何をしているか」だけでなく「**なぜそう設計したか**」と「**それをどのテストが守っているか**」を書く。
- ゴールは、読み終えた人が **自分で機能を1つ追加できる** こと。

## 言語と書き方

- **日本語で書く。** クラス名・メソッド名・ファイルパス・コマンド・SQL はコードの表記のまま（英語）。
- 説明には実際のコードを短く引用し、ファイルパスを添える（例: `backend/src/main/java/com/example/runbuddy/persistence/RlsTransactionManager.java`）。
  コードにないこと（未実装の機能、存在しないクラス）を書かない。
- 処理の流れやコンポーネントの関係は **Mermaid** の図にする。
  リクエストが層をまたぐ場面は `sequenceDiagram`、構造は `flowchart` / `classDiagram`。
- 設計判断は `docs/adr/` の ADR が正。ADR の内容を言い換えて終わらせず、ADR へのリンクを張ったうえで
  「その判断がコードのどこにどう現れているか」を書く。
- 重複を避ける。手順の詳細が `docs/guides/` にあるものは、要点とリンクにとどめる。

## 書いてほしいページ

### 1. アーキテクチャ概要

- monorepo（`backend/` = Spring Boot 4 / Java 25、`frontend/` = Vite + React + TanStack Router）の全体像。
- backend のパッケージ構成（`plan`, `activity`, `dashboard`, `day`, `race`, `advisor`, `export`, `persistence`, `shared`, `web`）と依存の向き。
  ドメインのパッケージが Spring・jOOQ・Spring AI に依存していないことを図で示す。
- 本番の形: `-Papp` で frontend を jar の `static/` に同梱し、1つのオリジンで配信する（`web/SpaConfig`）。
  手元のマシンで動かし Tailscale でつなぐ（ADR-0010）。

### 2. 永続化: jOOQ と行レベルセキュリティ（**詳しく**）

このプロジェクトでいちばん厚く書いてほしい部分。

- なぜ JPA / MyBatis / Spring Data JDBC / JdbcClient ではなく jOOQ なのか（ADR-0007 の比較を要約し、リンク）。
- **1回のリクエストで何が起きるか** を `sequenceDiagram` で:
  Controller → リポジトリのポート（例: `MonthlyPlanRepository`）→ jOOQ アダプタ（`JooqMonthlyPlanRepository`, `@Transactional`）
  → `RlsTransactionManager` がトランザクション開始時に `set_config('app.user_id', ..., true)`
  → jOOQ の DSL が SQL を発行 → PostgreSQL の RLS ポリシー（`own_rows`）が行を絞る → レコードからドメインの record へ変換。
- アプリの SQL が `user_id` に一切触れない理由と、それを支える仕組み:
  列のデフォルト値、`app_current_user_id()` が未設定なら例外を投げること、アプリは表の所有者ではない `runbuddy_app` で接続すること、
  Flyway は所有者 `runbuddy` で流すこと。
- ポートに `default` メソッドを置かない理由（`@Transactional` のプロキシの外で動いてしまい、実際にバグになった。ADR-0007）と、
  それを人の注意ではなく ArchUnit のテスト `TransactionalPortRulesTest` で検査していること（何を対象にし、どう失敗するか）。
  さらに、そのテスト自体が効いていることを、ミューテーションチェック（`backend/scripts/archunit-mutation-check.sh`）が
  違反コードを仕込んだ worktree で確かめていること。
- jOOQ のコード生成: `backend/scripts/generate-jooq.sh`、生成物 `persistence/jooq/generated` は手で直さないこと、
  CI が再生成して差分があれば落とすこと。生成クラスを使った DSL のクエリ例を、実際のアダプタから引用する。
- record（ドメイン）と jOOQ の Record（行）の対応づけを、どこで・どう書いているか。

### 3. その他のアーキテクチャ上の工夫

それぞれ「問題 → 工夫 → コードの場所 → 守っているテスト」の順で書く。

- **ポート & アダプタ**: `ActivitySource`（`DummyActivitySource` / `GarminActivitySource`）、
  リポジトリ（jOOQ / メモリ）、`TrainingAdvisor`（Spring AI / fake）。設定で差し替える仕組み（ADR-0004）。
- **契約テスト**: `MonthlyPlanRepositoryContract` などのインターフェースに振る舞いを1回だけ書き、
  jOOQ 版とメモリ版の両方のテストが実装する。`RowLevelSecurityContract` と `DatabaseSafetyTest` が
  ユーザー間のデータ漏れを防ぐ最後の砦であること。
- **不変のドメインモデル**: record / final クラスが自分の不変条件を検査する（例: `MonthlyPlan`, `Distance`, `export/DailyLog`）。
- **AI 相談（ADR-0008）**: Spring AI の `ChatClient` を `advisor/springai` に閉じ込める。
  ツール呼び出し（`AdvisorTools`、1回ごとに短いトランザクション）、構造化出力、
  モデルの提案は信用しない入力として `AdvisorAnswer.toPlan` を通すこと、モデル呼び出しをトランザクションの中でしないこと。
  月の途中の見直しでは、提案を保存済みの計画と `MonthlyPlan.revisedFrom` で合成し、今日より前の予定は残すこと（#31）。
  会話ごとにプロバイダとモデルを選べる仕組み（`AdvisorDirectory`）。
  AI が目標のレースを読むツール（`AdvisorTools.upcomingRaces`：残りの日数と週数、目標タイムと必要なペース）と、
  システムプロンプトでレースから逆算し、最後の 2〜3 週は量を落とす（テーパリング）よう指示していること。
- **Garmin 連携（ADR-0009）**: 外部 API の知識を `GarminActivitySource` に閉じ込め、失敗を理由つきの
  `ActivitySourceUnavailableException` に変換し、HTTP の ProblemDetail（`reason`）まで届ける流れ。
- **OpenAPI で型を共有**: springdoc の出力を `npm run gen:api` で TypeScript の型にする。
- **frontend**: ルートがデータを取り、コンポーネントは props だけを受ける（ネットワークなしでテストできる）。
  DESIGN.md のトークンとコンポーネントクラス（ADR-0006）。
- **ランの測定値と日別詳細ページ**: Garmin の一覧 API から取り込む最大心拍・ケイデンス・歩幅・上昇を、
  値オブジェクト `RunMetrics` にまとめ、自分で不変条件を検査すること（Garmin の変換では、おかしな値はその値だけを捨て、ランは残す）。
  その日の予定とランを `DayDetail` にまとめ、日別詳細ページ `/days/{日付}` で表示すること（1日に複数回走った日は並べる）。
- **ラップと軌跡（ADR-0012）**: FIT ファイルを初めて開いたときだけダウンロードし、原本を `run_fit_file` に保存して毎回読む設計。
  ポート `ActivityFileSource` と `ActivityFileReader`、FIT SDK を `activity/fit` に閉じ込めていること、ダウンロード中にトランザクションを開かないこと、
  テスト用の FIT を `SyntheticFit` で合成していること。
- **ランコースの地図（ADR-0013）**: Leaflet + OpenStreetMap。`RunDetail.route` が軌跡を約 300 点に間引く（スタート・ゴールと各ラップの最初の点は残す）。
  `RunRoute` は props だけを受け取り、位置のないランは地図もタイル要求も出さず「位置情報なし」と表示すること。
  ラップの行を押すと地図でその区間を強調する仕組み（`RunDetail.lapAt` で各点にラップ番号を付け、間引きでも各ラップの最初の点を残す。`RunDetailPanel` が選択を持ち、`lapSegment` で区間を切り出す）。
- **ダッシュボードの「いまやること」（ADR-0014）**: 状況の判定はドメイン（`SituationRules`、`TrainingSnapshot`、sealed interface の `Situation`）、
  API はカードの種類と数値だけ（`GET /api/next-steps`、`kind` で区別するユニオン型）、文言は frontend（`lib/nextSteps.ts`）。
  レースが近いあいだは目標からの遅れを出さないこと。カードは画面を開くだけで保存しないこと（AI 相談は `?ask=` で入力欄に入れるだけ）。
- **目標のレース**: `Race` の不変条件、目標タイムから必要なペースを切り捨てで出す理由（表示のペースで走れば目標に届く）、
  ダッシュボードのカウントダウンと、プラン画面での表示。
- **設定と秘密情報**: リポジトリ直下の `.env` を docker compose と Spring の両方が読む仕組み（`spring.config.import`）。

### 4. チュートリアル: 機能を1つ追加してみる

このリポジトリで実際に機能を追加する手順を、**テストを先に書く順番で** 通しで説明する。

- 題材は **既存の機能をなぞる** 形にする。月次の CSV エクスポート（`export` パッケージ、
  `GET /api/export/{YYYY-MM}.csv`、ダッシュボードの `ExportLink`）が、ドメインモデル（`DailyLog`）→ 整形（`DailyLogCsv`）
  → コントローラ → frontend の順に小さく作られているので、これを手本として各段階のテストとコードを示す。
- そのうえで、**DB のスキーマ変更を伴う機能追加** の手順を説明する（題材は読者が試しやすい小さなもの、
  例: ランに「メモ」列を足す。実装はされていないので「やってみる」課題として書き、存在するコードのように書かない）:
  1. Flyway のマイグレーションを新しく足す（既存のマイグレーションは直さない）。`user_id`・デフォルト・`own_rows` ポリシーの扱い。
  2. `./scripts/generate-jooq.sh` で jOOQ を再生成し、コンパイルエラーを手がかりに直す場所を見つける。
  3. 契約テストに振る舞いを足し、jOOQ 版とメモリ版の両方を通す。
  4. ドメインの record とコントローラ、OpenAPI → `npm run gen:api` → frontend。
  5. `./mvnw test`、`npm test`、`npm run build` で確認する。
- 各手順で「どのテストが赤くなり、何をすると緑になるか」を書く。

### 5. 開発の始め方とテストの回し方

- README と `docs/guides/` を要約し、リンクする（重複して詳しく書かない）。
- テストの種類（ドメインの単体テスト、`@WebMvcTest`、Testcontainers の DB テスト、Vitest + Testing Library）と、
  それぞれをいつ使うか。

## 書かないこと

- **秘密情報や個人情報を書かない。** API キー（`GEMINI_API_KEY`, `GARMIN_API_KEY` など）の値、`.env` の中身、
  実在のメールアドレス、個人の契約プラン、実際のランの記録（場所・日時・ID）を書かない。
  設定項目は名前と意味だけを書く。
- このリポジトリは public。外部に公開されて困ることは書かない。
- 生成コード（`persistence/jooq/generated`, `frontend/src/api/schema.gen.ts`, `frontend/src/routeTree.gen.ts`）を
  ページとして解説しない（生成の仕方と使い方だけを書く）。
