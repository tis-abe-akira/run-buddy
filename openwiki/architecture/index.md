# ファイル

- [アーキテクチャ概要](overview.md) - run-buddy の Monorepo 全体構造（Spring Boot 4 / Java 25 と Vite + React + TanStack Router）、バックエンドの 10 個のパッケージ構成と依存の向き、SpaConfig による単一オリジン統合配信、および Tailscale による個人アクセス運用モデルを解説する。
- [その他のアーキテクチャ上の工夫](patterns.md) - Ports & Adapters パターン、契約テスト、不変ドメインモデル、Spring AI 統合（AdvisorTools/AdvisorDirectory）、Garmin 連携、OpenAPI 型連携、フロントエンドコンポーネント設計、設定と秘密情報管理を解説する。
- [永続化: jOOQ と行レベルセキュリティ](persistence-rls.md) - なぜ jOOQ を採用したか、RlsTransactionManager と PostgreSQL RLS によるマルチテナント保護、port での default メソッド禁止と ArchUnit 検査、jOOQ コード生成と Record 変換パターンを解説します。
