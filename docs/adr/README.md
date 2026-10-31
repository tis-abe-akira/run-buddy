# Architecture Decision Records

設計上の重要な判断を記録します。書き方は [ADR-0001](0001-record-architecture-decisions.md) を参照してください。

| No. | タイトル | ステータス |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | ADRで設計判断を記録する | Accepted |
| [0002](0002-monorepo-frontend-backend.md) | frontend / backend のモノレポ構成にする | Accepted |
| [0003](0003-backend-spring-boot-java.md) | backend に Spring Boot 4 / Java 25 / Maven を使う | Accepted |
| [0004](0004-ports-and-adapters-for-external-services.md) | 外部サービス連携をポート&アダプタで分離し、Garminは最初ダミーにする | Accepted |
| [0005](0005-frontend-framework.md) | frontend に Vite + TanStack Router（SPA）を使う | Accepted |
| [0006](0006-design-system.md) | frontend のデザインを DESIGN.md のトークンで統一する | Accepted |
| [0007](0007-persistence-library.md) | 永続化ライブラリの選定（PostgreSQL + RLS）→ jOOQ | Accepted |
| [0008](0008-ai-integration.md) | AI相談機能の実装方式（Gemini / Claude / OpenAI / Bedrock を選択可能に）| Accepted（Spring AI） |

## テンプレート

```markdown
# ADR-XXXX: タイトル

- ステータス: Proposed | Accepted | Deprecated | Superseded by ADR-YYYY
- 日付: YYYY-MM-DD

## コンテキスト
## 決定
## 検討した選択肢
## 結果（トレードオフ）
```
