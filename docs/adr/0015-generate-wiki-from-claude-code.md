# ADR-0015: Wiki の生成を CI（Gemini）から Claude Code（Claude）に移す

- ステータス: Accepted
- 日付: 2026-11-07
- ADR-0011 を置き換える（Pages への公開と `openwiki/INSTRUCTIONS.md` の方針は引き継ぐ）

## コンテキスト

ADR-0011 では、main への push ごとに GitHub Actions が OpenWiki を Gemini で動かし、Wiki 更新の PR を作っていた。
使ってみると、**新しい機能が Wiki に載らない取りこぼしが続いた**。

- 差分更新（`update`）は、変わったコードに関係するページを部分的に書き直すだけで、新しい概念のページや節を足さないことが多い。
- 指示（message）を付けた手動実行でも、ページは書き足されなかった。
- たとえば `AdvisorTools.upcomingRaces`（AI がレースを読むツール、#16）は、初回の生成から一度も載っていない。
  ラップ・地図・ダッシュボードのカード・計画の見直しも、`INSTRUCTIONS.md` に書いたあとで欠けていた。
- CI で動くので、抜けに気づくのはマージ後に人が読んだときで、直す手段は「もう一度実行する」しかなかった。

OpenWiki（0.7.2 から）には **コーディングエージェント連携** がある（README の "Coding-agent integrations"）。
Claude Code に MCP サーバーとスキルを入れると、Claude Code のセッションのモデル（Claude）がリポジトリを調べてページを書き、
OpenWiki は進行（ページのキュー）、根拠（Claims）の検証、索引・来歴の生成を受け持つ。
**Claude Code のログインをそのまま使うので、別の API キーは要らない。** 差分更新（`openwiki_begin` の `mode: update`）にも対応している。

## 決定

1. **Wiki は Claude Code から更新する。** 機能をマージしたら、Claude Code に「Wiki を更新して」と頼む。
   - 連携はプロジェクト単位で入れ、リポジトリにコミットする：`.mcp.json`（`openwiki mcp --host claude`）と
     `.claude/skills/openwiki/`（`openwiki integrations install claude --project` が生成。手で直さない）。
     どの Claude Code セッションでも、このリポジトリを開けば使える。
   - `openwiki` コマンドが PATH に要る。Mac では `npm install -g openwiki@0.7.2`。
     Claude Code のクラウド環境では、環境のセットアップスクリプトで同じものを入れる。
   - バージョンは 0.7.2 に固定する（今の Wiki を生成したもの）。上げるときは連携を入れ直し、ガイドも直す。
2. **抜けは機械で見つける。** `scripts/openwiki-coverage.sh` が、`INSTRUCTIONS.md` に挙げたクラス名・メンバー名・ADR 番号が
   Wiki のどこかに出てくるかを調べ、出てこないものを一覧にして失敗する。更新のたびに実行し、抜けがあれば
   その場で Claude に書き足させる（ページの内容は OpenWiki の手順の中で直す。生成物を手で編集しない）。
3. **CI の Wiki 生成（`.github/workflows/openwiki-update.yml`）はやめる。** GitHub の Secrets の `GEMINI_API_KEY`（Wiki 用）と
   `OPENWIKI_PR_TOKEN` も不要になる。
4. **GitHub Pages への公開（`openwiki-pages.yml`）は残す。** `openwiki/` の変更が main に入ると公開される（API キー不要）。

## 検討した選択肢

| 案 | 判断 |
|---|---|
| A. CI で Gemini のまま続ける（ADR-0011） | 取りこぼしが直らない。抜けに気づいても直す手段が再実行しかない |
| B. CI の OpenWiki のプロバイダを Anthropic にする（`OPENWIKI_PROVIDER=anthropic`） | 自動のままモデルは変えられるが、Anthropic API のキー（従量課金）が別に要る。抜けを途中で直せない点は A と同じ |
| **C. Claude Code 連携で、Claude Code から更新する**（採用） | キー不要。書きながら `INSTRUCTIONS.md` と照らして抜けを埋められる。更新は人が頼む必要がある |

## 結果（トレードオフ）

- 良い点：書くのがその場の Claude なので、抜けや誤りに気づいたら同じ実行の中で直せる。網羅チェックで抜けが数字で見える。
- 良い点：Gemini へのソースの送信、Wiki 用の Secrets と PAT の管理がなくなる。
- 注意点：更新は自動ではない。機能の PR をマージしたら頼む（Issue 駆動の流れの最後の1手として行う）。
- 注意点：Claude のプランの利用枠を使う。Wiki 全体の作り直し（`init`）は重いので、普段は差分更新にする。
- 注意点：生成中に Claude がソースを変えないことは、OpenWiki のスキルの規則（"Never modify source code while generating the wiki"）に従う。
