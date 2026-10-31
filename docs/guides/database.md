# データベース（PostgreSQL）ガイド

ローカルの PostgreSQL の中身を見る方法、行レベルセキュリティ（RLS）の仕組み、スキーマを変えるときの手順をまとめます。
設計判断の背景は [ADR-0007](../adr/0007-persistence-library.md) を参照してください。

コマンドはすべて、**リポジトリのルート**（`compose.yaml` がある場所）で実行します。手元に `psql` がなくても、PostgreSQL のコンテナの中にある `psql` を `docker compose exec` で使えます。

## 構成

### テーブル

| テーブル | 内容 | 主キー |
|---|---|---|
| `monthly_plan` | 月次プラン（月間目標） | `user_id`, `month` |
| `planned_workout` | プランの予定ワークアウト | `user_id`, `month`, `position` |
| `run_result` | ランの記録 | `user_id`, `activity_id` |
| `flyway_schema_history` | Flyway が使う、マイグレーションの適用履歴 | |

- 距離はメートル（`*_meters`）、時間はミリ秒（`duration_millis`）で持っています。km で見るときは 1000 で割ります。
- スキーマの定義は `backend/src/main/resources/db/migration/` にあります。

### ユーザーごとのデータの分け方（RLS）

- どのテーブルにも `user_id` 列がありますが、**アプリの SQL は `user_id` に一切触れません。**
- backend は、トランザクションの最初に `set_config('app.user_id', <ユーザーID>, true)` を実行します（`RlsTransactionManager`）。
  - 行を追加するときは、`user_id` 列にこの値が既定値として入ります。
  - 行を読み書きするときは、RLS のポリシーが「`user_id` がこの値の行」だけに絞り込みます。
- ログイン機能はまだないので、今は全員がユーザー `demo` です（`runbuddy.default-user-id`）。

### ロール（DB のユーザー）

| ロール | 使い道 | RLS |
|---|---|---|
| `runbuddy` | テーブルの所有者。Flyway のマイグレーションと、手で中身を確認するとき | **効かない**（スーパーユーザーなので、全ユーザーの行が見える） |
| `runbuddy_app` | アプリ（backend）が接続するとき | **効く**（`app.user_id` が必要） |

`runbuddy_app` は、コンテナの初回起動時に `backend/db/init/01-create-app-role.sql` が作ります。パスワードはどちらもローカル開発専用です。

## 中身を見る

### 作ったプランを見る

プランとワークアウトを1つの表で表示します。

```bash
docker compose exec postgres psql -U runbuddy -d runbuddy -c "select p.user_id, p.month, round(p.target_meters / 1000.0, 1) as target_km, w.workout_date, w.type, round(w.distance_meters / 1000.0, 1) as km, w.note from monthly_plan p left join planned_workout w on w.user_id = p.user_id and w.month = p.month order by p.month, w.position;"
```

```
 user_id |  month  | target_km | workout_date | type |  km  | note
---------+---------+-----------+--------------+------+------+------
 demo    | 2026-11 |     200.0 | 2026-11-03   | EASY |  6.0 |
 demo    | 2026-11 |     200.0 | 2026-11-08   | LONG | 20.0 | LSD
```

### ランの記録を月ごとに集計する

```bash
docker compose exec postgres psql -U runbuddy -d runbuddy -c "select user_id, to_char(started_at, 'YYYY-MM') as month, count(*) as runs, round(sum(distance_meters) / 1000.0, 1) as km from run_result group by 1, 2 order by 1, 2;"
```

### 対話モードで自由に見る

```bash
docker compose exec postgres psql -U runbuddy -d runbuddy
```

| コマンド | 内容 |
|---|---|
| `\dt` | テーブルの一覧 |
| `\d monthly_plan` | テーブルの定義（RLS のポリシーも表示される） |
| `\df app_current_user_id` | 現在のユーザーIDを返す関数 |
| `select * from run_result order by started_at limit 10;` | 中身を見る（行末の `;` を忘れずに） |
| `\x` | 縦長表示の切り替え（列が多いときに便利） |
| `\q` | 終了 |

## RLS の動きを確かめる

アプリと同じ `runbuddy_app` で接続すると、RLS が効きます。

**ユーザーを指定しないと、エラーで止まります。** トランザクションを付け忘れたコードが、黙って0件を返さずにすぐ壊れるようにしてあります。

```bash
docker compose exec postgres psql -U runbuddy_app -d runbuddy -c "select * from monthly_plan;"
```

```
ERROR:  app.user_id is not set; database access must run in a transaction (RlsTransactionManager)
```

**トランザクションの中でユーザーを指定すると、そのユーザーの行だけが見えます。**

```bash
docker compose exec -T postgres psql -U runbuddy_app -d runbuddy <<'SQL'
begin;
select set_config('app.user_id', 'demo', true);
select month, target_meters / 1000.0 as target_km from monthly_plan;
commit;
SQL
```

`'demo'` を `'someone-else'` に変えると 0 件になります。SQL は `user_id` を指定していないのに、DB 側でユーザーごとに分かれていることが分かります。

## スキーマを変える

1. `backend/src/main/resources/db/migration/` に **新しい** マイグレーションファイルを追加します（例: `V2__add_elevation_to_runs.sql`）。マージ済みのマイグレーションは書き換えません。
2. 新しいテーブルには、次の3つを必ず付けます。書き方は V1 と同じです。
   - `user_id` 列（既定値 `app_current_user_id()`）
   - RLS の有効化（`enable` と `force`）
   - `own_rows` ポリシー
3. jOOQ のコードを生成し直します（Docker が必要です）。

   ```bash
   cd backend
   ./scripts/generate-jooq.sh
   ```

4. コンパイルエラーになった箇所を直します。スキーマの変更で影響を受ける場所が、そのまま一覧になります。
5. 生成されたコード（`persistence/jooq/generated`）も一緒にコミットします。CI は生成し直したコードとの差分をチェックしており、差分があると失敗します。

backend を次に起動したとき、Flyway が新しいマイグレーションを自動で適用します。

## DB アクセスの約束事（backend のコードを書くとき）

- DB へのアクセスは**必ずトランザクションの中で**行います。アダプタには `@Transactional` を付けます（読み取りは `readOnly = true`）。
- ポート（リポジトリのインターフェース）に `default` メソッドを書きません。default メソッドは `@Transactional` のプロキシを通らず、トランザクションの外で実行されてしまいます（実際に起きたバグです。ADR-0007 を参照）。
- LLM の呼び出しなど時間のかかる処理を、トランザクションの中で行いません。
- テストでも、アプリは `runbuddy_app` で接続します。`RowLevelSecurityContract` と `DatabaseSafetyTest` は削除・スキップしません。

詳しくは [backend/CLAUDE.md](../../backend/CLAUDE.md) を参照してください。

## ダミーのランだけを消す

ダミー（`RUNBUDDY_ACTIVITY_SOURCE=dummy`）で取り込んだあとに Garmin に切り替えると、同じ月にダミーのランと Garmin のランが両方残り、合計距離が二重になります（ADR-0009）。どちらから取り込んだかは `activity_id` の先頭で分かります（`dummy-…` / `garmin-…`）。

取り込み元ごとの件数を確かめます。

```bash
docker compose exec postgres psql -U runbuddy -d runbuddy -c "select user_id, split_part(activity_id, '-', 1) as source, count(*) from run_result group by 1, 2 order by 1, 2;"
```

ダミーのランだけを消します。所有者の `runbuddy` で接続しているので、RLS に関係なく全ユーザーのダミーのランが消えます。プランや Garmin のランはそのまま残ります。

```bash
docker compose exec postgres psql -U runbuddy -d runbuddy -c "delete from run_result where activity_id like 'dummy-%';"
```

## 作り直す・ポートを変える

**データをすべて消して作り直す:**

```bash
docker compose down -v
```

```bash
docker compose up -d --wait
```

**5432 番が使えない:** リポジトリのルートの `.env` に `RUNBUDDY_DB_PORT=5433` のように書きます。Docker Compose と backend の両方が `.env` を読むので、どちらも同じポートを使います。`docker compose exec` はポート番号に関係なく使えます。
