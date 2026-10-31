# ADR-0009: Garmin Connect Personal API でランの実績を取り込む

- ステータス: Accepted
- 日付: 2026-10-31

## コンテキスト

Phase 4（concept.md UC-6）では、ダミーの `ActivitySource`（ADR-0004）を本物の Garmin につなぐ。

2026-10-21 に Garmin Connect Developer Program が再開し、個人が自分のデータを読むための **Personal API** ができた。申請は要らず、Garmin Connect Web の「Personal API Key」ページでキーを発行する。読み取り専用で、Training API（ワークアウトの送信など）は含まれない。run-buddy は「自分の記録を自分で振り返る」アプリなので、この枠が合う。

### 調べて分かったこと（2026-10-31 に Swagger・FAQ を読み、実際の API で確認）

参照: [Personal API](https://developer.garmin.com/gc-developer-program/personal/personal-api/)、[FAQ](https://developer.garmin.com/gc-developer-program/personal/personal-faq/)、[Swagger](https://developer.garmin.com/gc-developer-program/personal/swagger-docs)（OpenAPI 3.0.1。ページの JavaScript に埋め込まれている）

**認証**

- キーでは直接データを読めない。`POST https://personal.connectapi.garmin.com/personalapitoken-service/personal-api/personal-api-key/token` に `Authorization: ApiKey <key>` を付けて送り、アクセストークンと交換する。
  - レスポンスは **JSON ではなく `text/plain` で、本文がそのままトークン（JWT）**。この形は Swagger にも FAQ にも書かれておらず、実際に呼んで確認した。
  - トークンの有効期間は 24 時間、またはキーが失効するまでの短い方（FAQ）。実際のトークンの `exp` も、発行から 24 時間後だった。リフレッシュトークンはなく、期限が来たらキーでもう一度交換する。
- データの取得には `Authorization: Bearer <token>` を付ける。
- キーの有効期間は、作るときに 30 日・90 日・180 日・1 年から選ぶ。失効の約 1 週間前にメールが届く。キーは作成時に一度しか表示されない。
- 誤ったキーで交換しようとすると `401`（本文なし）。無効なトークンでデータを取ろうとすると `401`（`WWW-Authenticate: Bearer error="invalid_token"`）が返る。Authorization ヘッダーがないと `400`。

**アクティビティの一覧** `GET /activitylist-service/activities/search/activities`

- `startDate` / `endDate`（`YYYY-MM-DD`、両端を含む）。**アクティビティの現地時刻の開始日**で絞り込む。
- ページング: `start`（0 始まりのオフセット）と `limit`（既定 99。最大値は書かれていない。200 は受け付けられた）。結果がなければ空の配列。
- `sortOrder`（`asc` / `desc`、既定 desc）、`sortBy`（既定は現地時刻の開始日時）。
- `activityType`（親の種別だけ。例: `running`）、`activitySubType`（例: `trail_running`）。サブタイプのキーを `activityType` に渡すと `400`。
- 返ってくる各要素（`SummarizedActivityDTO`、約 230 項目）のうち、使うのは次の項目。
  - `activityId`（int64）
  - `startTimeLocal`（`yyyy-MM-dd HH:mm:ss`、**記録した場所の現地時刻で、UTC オフセットなし**）。`startTimeGMT` も同じ形式で UTC の時刻。一覧の要素には**タイムゾーン名の項目がない**（日本時間なら GMT との差は 9 時間）。
  - `activityType` = `{typeId, typeKey, parentTypeId}`。ランニングは `{1, "running", 17}`。`parentTypeId` は親の `typeId` を指し、17 は「自分が親」という意味（Swagger の説明）。
  - `distance`（メートル、double）、`duration`（タイマーの時間で秒、double。一時停止は含まない。`elapsedDuration` は経過時間）、`averageHR`（bpm、double。ない場合は null）
  - 2026-10-31 追加: `maxHR`（bpm）、`averageRunningCadenceInStepsPerMinute` / `maxRunningCadenceInStepsPerMinute`（両足の歩数/分）、`avgStrideLength`（cm）、`elevationGain`（m）。いずれも double で、実データ（2026-10 のラン 25 件）ではすべてに値があった。取り込み時に四捨五入して `RunMetrics` に入れる。0 や「最大 < 平均」はその値だけを捨て、ランは残す。
- 位置（`startLatitude` など）、所有者の氏名（`ownerFullName`）、地名（`locationName`）といった個人情報も入っている。run-buddy はこれらを読まない。

**レート制限**: Swagger にも FAQ にも書かれていない。今回の確認（約 15 リクエスト）では、レート制限に関係するレスポンスヘッダーは見当たらなかった。

**実データで分かったこと**（オーナーの 2026-10、32 件）

- ランニング 31 件、`fitness_equipment` 1 件。
- 手入力のアクティビティ（`manualActivity: true`）がある。そのうち 2 件は 0 m・0 秒で、`RunResult` の不変条件（距離も時間も正）を満たさない。
- 2025-01 からの履歴 200 件を見たところ、すべて `running` で、サブタイプ（`trail_running` など）の実例はなかった。

## 決定

- `activity/garmin` パッケージにアダプタ `GarminActivitySource` を置き、`runbuddy.activity-source=garmin` で有効にする（環境変数 `RUNBUDDY_ACTIVITY_SOURCE`）。ポート `ActivitySource` は変えない。
- HTTP には Spring の `RestClient` を使う（新しい依存はない）。
- **取得範囲**: その月の 1 日〜末日を `startDate` / `endDate` で指定し、`sortOrder=asc`、`limit=99` でページを読む。返ってきた件数が 99 未満になったら終わり。古い順に並べるのは、取得している途中に新しいアクティビティが増えても、すでに読んだオフセットがずれないようにするため。念のため `activityId` で重複を除き、ページ数の上限（20 ページ）も設ける。
- **ランニングの判定**: 種別ではサーバー側で絞らず、アダプタの中で `typeKey == "running"` または `parentTypeId == 1`（running の typeId）のものを採る。サーバーの `activityType=running` がサブタイプを含むかどうかは確認できなかったため。
- **変換**:
  - `activityId` → `"garmin-<activityId>"`。ダミーの ID（`dummy-<日付>`）とぶつからない。取り込みは今までどおり ID で upsert するので、何度実行しても重複しない。
  - 距離はメートル単位に丸め、時間は `duration`（Garmin Connect の画面で「タイム」と表示される値）をミリ秒に丸め、心拍は四捨五入する。
  - 距離か時間が 0 のもの（0 m の手入力など）は取り込まない。
- **タイムゾーン**: `RunResult.startedAt`（`LocalDateTime`）には `startTimeLocal`、つまり**走った場所の現地時刻**をそのまま入れる。ダッシュボードの「何日に走ったか」はその土地の日付で数えるのが自然で、Garmin の `startDate` / `endDate` もこの現地時刻の日付で絞り込むので、月の範囲と一致する。旅先で走っても、その土地の朝のランはその土地の日付になる。
- **トークン**: `GarminTokens` が交換したトークンを 23 時間（24 時間より 1 時間短く）使い回す。データ取得が `401` を返したら、トークンを捨てて 1 回だけ取り直す。それでも `401`、またはトークンの交換自体が `401` / `403` なら、キーが拒否されたと判断する。
- **エラー**: アダプタは `ActivitySourceUnavailableException`（ドメイン側の例外）に理由を付けて投げる。`POST /api/runs/{month}/import` は ProblemDetail（RFC 9457）で返し、`reason` プロパティに理由を入れる。frontend は `reason` を見て日本語のメッセージを出す。

  | 理由 | 起きること | HTTP | 画面の案内 |
  |---|---|---|---|
  | `CREDENTIALS_REJECTED` | キーの期限切れ・削除・誤り（401 / 403） | 502 | 新しいキーを作って `GARMIN_API_KEY` を書き換え、backend を起動し直す |
  | `RATE_LIMITED` | Garmin が 429 を返した（`Retry-After` があれば詳細に含める） | 429 | しばらく待ってから取り込み直す |
  | `UNAVAILABLE` | 接続できない、タイムアウト、5xx、読めないレスポンス | 502 | ネットワークを確認し、時間をおいて取り込み直す |

- **キーが空のとき**: `runbuddy.activity-source=garmin` なのに `GARMIN_API_KEY` が空なら、**起動を失敗させる**（理由と直し方をメッセージに出す）。ダミーにフォールバックしない。garmin を選んだのにダミーで動くと、生成したランが本物のランと同じ DB に入り、画面でも区別がつかないため。キーは起動時には検証しない（起動のたびに Garmin を呼ばないため）。キーが無効なら、取り込みのときに `CREDENTIALS_REJECTED` になる。
- **画面の表示**: `GET /api/activity-source` で今の取得元（`dummy` / `garmin`）を返し、「ダミーデータ」のバッジは dummy のときだけ出す。
- **テスト**:
  - `GarminActivitySourceTest`: `MockRestServiceServer` で HTTP をモックする。変換、ページング、重複の除去、トークンの使い回し・期限切れ・401 での取り直し、各エラー。
  - 録画したレスポンス `src/test/resources/garmin/activities-2026-10.json` は、実際のレスポンスの**形**（項目名・型・null の出方・手入力や 0 m の例）をまねて作ったもの。ID・名前・数値は作り物で、位置・氏名・地名は入れていない。
  - `GarminLiveTest`: 本物の API を呼ぶ。OS の環境変数 `GARMIN_API_KEY` があるときだけ動く（`GeminiLiveTest` と同じ方針）。

## 検討した選択肢

| 案 | 判断 |
|---|---|
| A. Personal API の JSON 一覧（採用） | 必要な項目（開始時刻・距離・時間・平均心拍・種別・ID）が 1 回の呼び出しで、月の範囲・ページ単位で取れる |
| B. `.FIT` ファイル（`/download-service/files/activity/{id}`）を 1 件ずつダウンロードして解析する | 1 件ごとに 1 リクエスト必要で、FIT の解析ライブラリも要る。ラップや心拍の時系列が必要になったときに考える |
| C. `wellnessactivity-service/activity/summary/range` | 日ごとの健康データ向けで、ランの記録には A の方が直接的 |
| D. 非公式ライブラリ（Garmin Connect の Web 用 API をログイン情報で叩く） | 規約上グレーで、パスワードを預かることになる。公式の Personal API ができたので採らない |
| キーが空のときダミーにフォールバック | 上の「キーが空のとき」のとおり、本物とダミーが混ざるので採らない |

## 結果（トレードオフ）

- 良い点: オーナー自身の記録を、ダッシュボードの「Garminから取り込む」でそのまま取り込める（2026-09: 37 件、2026-10: 29 件を確認）。
- 良い点: ポートを変えていないので、ダミーとの切り替えは設定だけで済む。ドメインやほかのテストには影響しない。
- 注意点: キーは最長 1 年で失効する。期限が来たら手動で作り直して `.env` を書き換える必要がある（画面にもその案内を出す）。
- 注意点: Personal API は「本人のデータを本人が使う」ための枠（FAQ: 複数のエンドユーザー向けの商用アプリは対象外）。run-buddy を他の人にも使ってもらうなら、Business 向けのプログラム（OAuth、ユーザーごとの同意）に移る必要がある。

## 未解決・未確認

- **利用規約（Personal API の Terms and Conditions）は未確認。** キーを作るときに Garmin Connect Web（ログインが必要）で表示されて同意するもので、developer.garmin.com には公開されていない。とくに、**取得したデータを第三者の AI（Gemini、ADR-0008）に送ってよいかは確認できていない。** AI 相談では、ランの記録（日付・距離・時間・心拍）が Google に送られる。オーナーが Garmin Connect Web で規約を読んで確認する必要がある。
- レート制限の値は分からない。`429` が返れば `RATE_LIMITED` として扱うが、実際に 429 が返るかどうかと、そのときのレスポンスの形は確認していない。
- ランニングのサブタイプ（`trail_running`、`treadmill_running` など）の実例はまだ見ていない。判定は「`parentTypeId` が親の `typeId` を指す」という Swagger の説明と、running の `typeId` が 1 だったことに基づく。
- マルチスポーツ（トライアスロンなど）のラン区間は、`excludeChildren` の既定値（true）によって取り込まれない。
- ダミーで取り込んだあとに garmin へ切り替えると、同じ月にダミーのラン（`dummy-…`）と本物のラン（`garmin-…`）が両方残り、合計距離が二重になる。今は消す API がないので、DB を作り直す（`docker compose down -v`）か、`run_result` から `dummy-` で始まる行を消す必要がある（[データベースガイド](../guides/database.md)）。
- 取り込みは手動（ボタン）だけ。定期実行や、前回からの差分だけを取る仕組みはまだない。
