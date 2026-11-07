# ファイル

- [アーキテクチャ概要](overview.md) - run-buddy の Monorepo 全体構造（Spring Boot 4 / Java 25 と Vite + React + TanStack Router）、バックエンドの 10 個のパッケージ構成と依存の向き、SpaConfig による単一オリジン統合配信、および Tailscale による個人アクセス運用モデルを解説する。
- [アーキテクチャ上の工夫と設計パターン](patterns.md) - Ports & Adapters、契約テスト、不変ドメインモデル、Spring AI相談連携、Garmin連携・FIT解析、Leaflet地図表示、ダッシュボード判定ロジック、設定管理など、システム全体の設計工夫と品質保護メカニズムを解説する。
- [永続化: jOOQ と行レベルセキュリティ](persistence-rls.md) - なぜ jOOQ を採用したか、RlsTransactionManager と PostgreSQL RLS によるマルチテナント保護、port での default メソッド禁止と ArchUnit 検査、jOOQ コード生成と Record 変換パターンを解説します。
