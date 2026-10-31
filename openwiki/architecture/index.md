# ファイル

- [アーキテクチャ概要](overview.md) - Monorepo構成（Spring Boot 4 / Java 25 & Vite + React）、バックエンドのパッケージ構成、ドメイン中心の依存制御、SpaConfigによるフロントエンド静的配信、およびTailscaleを用いた動作モデルを解説する。
- [アーキテクチャパターンと設計の工夫](patterns.md) - ポート＆アダプタパターン、契約テストによる実装検証、不変ドメインモデル、Spring AIを用いたAdvisor機能、Garmin連携での例外・ProblemDetailハンドリング、OpenAPI型共有、フロントエンド設計の意図と実装方法を解説。
- [永続化: jOOQ と行レベルセキュリティ](persistence-rls.md) - PostgreSQLの行レベルセキュリティ（RLS）によるマルチテナントデータ分離機構、RlsTransactionManagerでのset_config処理、jOOQを使用した型安全なデータアクセス、コード生成フロー、およびデータベース安全性テストを詳述する。
