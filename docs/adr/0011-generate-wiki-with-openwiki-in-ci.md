# ADR-0011: OpenWiki で Wiki を CI から自動生成し、GitHub Pages に公開する

- ステータス: Accepted
- 日付: 2026-10-31

## コンテキスト

README・ADR・ガイドはあるが、「アーキテクチャ全体がどう動くか」（特に jOOQ と行レベルセキュリティの連携）や
「機能をどう足すか」を通しで説明する文書がない。手で書くと、コードが変わるたびに古くなる。

[OpenWiki](https://github.com/langchain-ai/openwiki)（LangChain、MIT）は、リポジトリを読んで `openwiki/` に Markdown の Wiki を書き、
前回からの差分だけを更新できる CLI。人間は `openwiki/INSTRUCTIONS.md` に方針（読者、書くページ、書かないこと）を書く。

## 検討した選択肢

| 案 | 内容 | 判断 |
|---|---|---|
| A. 手元で Claude Code から OpenWiki を動かす | 必要なときに `openwiki --update` を実行する | 兄弟リポジトリ（rls-sample）で試している。更新を忘れると古くなる |
| **B. CI で変更を見張って更新する**（採用） | main への push ごとに GitHub Actions が `--update` を実行し、PR を作る | 更新が自動。変更は PR で人がレビューしてから入る |

B のなかで決めたこと:

- **LLM:** Gemini（AI Studio）。アプリの AI 相談で使っているキーをそのまま使え、追加の契約が要らない。
  無料枠のレート制限で止まりやすいので、1ページずつ・再試行多めで動かし、途中で止まってもそれまでのページを PR に残す。
- **トリガー:** main への push ごと（`openwiki/` 自体の変更では動かさない。INSTRUCTIONS.md の変更では動かす）と手動実行。
- **PR を作るトークン:** fine-grained の Personal Access Token（`OPENWIKI_PR_TOKEN`）。
  `GITHUB_TOKEN` で作った PR では他のワークフローが動かず、ルールセットが必須にしている `backend` / `frontend` のチェックが走らない。
- **見る方法:** OpenWiki 付属のビューアーを使う。手元では `openwiki visualize`（サーバー）、
  公開用には `openwiki visualize --export` で静的サイトに書き出し、GitHub Pages に置く。
  MkDocs などの別の静的サイトジェネレーターは、設定と依存が増えるわりに得るものが少ないので使わない。
- **言語:** 日本語（`--language ja` と INSTRUCTIONS.md の指示）。

## 決定

- `openwiki/INSTRUCTIONS.md`（方針）と `.openwikiignore`（読ませないパス）をリポジトリに置く。
- `.github/workflows/openwiki-update.yml`（生成して PR を作る）と `.github/workflows/openwiki-pages.yml`（Pages に公開する）。
  Claude の GitHub App はワークフローを push できないので、Claude が `docs/ci/` に用意し、オーナーが移動した。手順は [docs/guides/openwiki.md](../guides/openwiki.md)。
- `openwiki/` の生成物は手で直さない。直したいときは INSTRUCTIONS.md かソースを直す。

## 結果（トレードオフ）

- 良い点: コードと一緒に Wiki が育つ。変更は PR で確認できる。コーディングエージェントも `AGENTS.md` 経由で Wiki を読める。
- 悪い点:
  - LLM が書くので、誤りが入りうる。INSTRUCTIONS.md で「コードにないことを書かない」「コードを引用する」と指示し、PR で人が確認する。
  - リポジトリ（public）のソースが Gemini に送られる。`.env` などは `.openwikiignore` で除外する。
  - Wiki も公開される。秘密情報・個人情報を書かないよう INSTRUCTIONS.md で指示し、マージ前に確認する。
  - PAT の有効期限が切れると PR が作れなくなる。
  - OpenWiki は新しく変化の速いツールなので、バージョンを固定（0.7.2）し、上げるときに確かめる。
