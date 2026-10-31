# ファイル

- [アーキテクチャ概要](overview.md) - run-buddy の Monorepo 全体構造、Spring Boot 4 と React 19 の層状設計、ドメインモデルの独立性、本番単一オリジン配信 (SpaConfig)、および Tailscale による個人アクセス運用モデルを解説する。
- [アーキテクチャ上の工夫と設計パターン](patterns.md) - ポート＆アダプタ、契約テスト、不変ドメインモデル、Spring AI 統合、Garmin 連携、OpenAPI 型連携、フロントエンド構造の解説。
- [永続化: jOOQ と行レベルセキュリティ (RLS)](persistence-rls.md) - jOOQ 選定理由、RlsTransactionManager による PostgreSQL RLS (own_rows) 適用とアプリ SQL の分離、ArchUnit によるトランザクション境界検証メカニズムを解説します。
