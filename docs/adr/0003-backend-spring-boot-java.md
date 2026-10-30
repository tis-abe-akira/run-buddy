# ADR-0003: backend に Spring Boot 4 / Java 25 / Maven を使う

- ステータス: Accepted
- 日付: 2026-10-30

## コンテキスト

このプロジェクトには「Java を使ったシステム開発を Claude Code で自動化する」という目的もあります。
オブジェクト指向での設計と、テストを厚く書く開発スタイルを試せる構成にしたいと考えています。

## 決定

- 言語: Java 25（LTS）
- フレームワーク: Spring Boot 4.1 系
- ビルド: Maven（`spring-boot-starter-parent` を継承する）
- テスト: JUnit 5。Web の層は `@WebMvcTest` + MockMvc（`spring-boot-starter-webmvc-test`）
- パッケージ: `com.example.runbuddy` の下を機能（`health`, `plan`, `activity` など）ごとに分ける（package by feature）

## 結果（トレードオフ）

- 良い点: `record`、sealed interface、パターンマッチングなどの新しい Java の機能でドメインモデルを素直に書ける。Spring のテスト支援が充実している。
- 注意点: Spring Boot 4 では、3 系からテスト用スターターやアノテーションのパッケージが変わっている（例: `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`）。ネット上の情報やAIの出力が古い書き方になっていることがあるので注意する。
- 注意点: 開発環境と CI に JDK 25 を用意しておく必要がある。
