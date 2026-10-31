---
type: concept
title: アーキテクチャ概要
description: Monorepo構成（Spring Boot 4 / Java 25 & Vite + React）、バックエンドのパッケージ構成、ドメイン中心の依存制御、SpaConfigによるフロントエンド静的配信、およびTailscaleを用いた動作モデルを解説する。
tags: [architecture, monorepo, backend, spring-boot, frontend, vite, spa-config, tailscale]
verified:
  - by: openwiki/0.7.2
    at: 2026-10-31T22:55:43.691Z
sources:
  - id: openwiki-source-cc9ac771b9086a58897b0e1c
    resource: repo://backend/pom.xml
  - id: openwiki-source-3f0e96c3aca5e3cd21848274
    resource: repo://backend/src/main/java/com/example/runbuddy/activity/RunResult.java
  - id: openwiki-source-0585f202c5c2fa2ae22e8332
    resource: repo://backend/src/main/java/com/example/runbuddy/advisor/TrainingAdvisor.java
  - id: openwiki-source-a2825a1ee16b80aa76ea01ed
    resource: repo://backend/src/main/java/com/example/runbuddy/day/DayController.java
  - id: openwiki-source-7f5aac3bed1693077b918720
    resource: repo://backend/src/main/java/com/example/runbuddy/day/DayDetail.java
  - id: openwiki-source-643bcac50f2f84a7718ec3ad
    resource: repo://backend/src/main/java/com/example/runbuddy/plan/MonthlyPlan.java
  - id: openwiki-source-a1489a590b1a811b9220a280
    resource: repo://backend/src/main/java/com/example/runbuddy/plan/MonthlyPlanRepository.java
  - id: openwiki-source-009ed5ea81e2ba3fd68f2231
    resource: repo://backend/src/main/java/com/example/runbuddy/web/SpaConfig.java
  - id: openwiki-source-97ae9adf7f31e5151e176dc2
    resource: repo://backend/src/main/resources/application.properties
  - id: openwiki-source-bfe274f6ff5a1796c7b09d72
    resource: repo://backend/src/test/java/com/example/runbuddy/web/SpaConfigTest.java
  - id: openwiki-source-99bfed725a9426a616829dc5
    resource: repo://docs/adr/0002-monorepo-frontend-backend.md
  - id: openwiki-source-f3132457ef2ed039f06dc400
    resource: repo://docs/adr/0003-backend-spring-boot-java.md
  - id: openwiki-source-1cce5cd6dd402146707a2c7b
    resource: repo://docs/adr/0010-run-at-home-over-tailscale.md
  - id: openwiki-source-1047363cf615000e4c9bb694
    resource: repo://frontend/package.json
generated: { by: "openwiki/0.7.2", at: "2026-10-31T22:55:43.691Z" }
---

run-buddy は、ランニングの月間計画（Plan）作成、走行実績（Activity）の自動同期、ダッシュボード表示、日詳細（Day）表示、AI によるトレーニング相談（Advisor）、および CSV エクスポートを提供する個人向け Web アプリケーションです。

1 人の開発者（および AI エージェント）による高い開発速度と保守性を実現するため、モノレポ構成、バックエンドのパッケージ設計、単一オリジン配信モデル、ネットワーク接続モデルにおいて一貫したアーキテクチャ方針を採用しています。

---

## モノレポ全体像と技術スタック

本プロジェクトは、Web UI と REST API を 1 つのリポジトリで管理するシンプル構造のモノレポ構成を採用しています（[ADR-0002](../../docs/adr/0002-monorepo-frontend-backend.md)）。複雑なモノレポ管理ツール（Nx や Turborepo など）は導入せず、エコシステムごとの標準ビルドツールでディレクトリを分割しています。

```
run-buddy/
├── backend/    # Spring Boot 4 / Java 25 (Maven プロジェクト)
├── frontend/   # Vite + React 19 + TanStack Router (Node.js / npm プロジェクト)
└── docs/       # 設計ドキュメント・ADR (Architecture Decision Records)
```

### 技術スタック概要

| 領域 | 技術・ライブラリ | 採用理由・役割 |
|---|---|---|
| **Backend** | Java 25 (LTS), Spring Boot 4.1.2 | `record` や `sealed interface` を用いた堅牢なドメインモデル設計（[ADR-0003](../../docs/adr/0003-backend-spring-boot-java.md)） |
| **Frontend** | React 19, Vite, TanStack Router, TanStack Query | 型安全なルーター・状態管理および OpenAPI スキーマからの型自動生成 (`openapi-typescript`) |
| **Persistence** | PostgreSQL, Flyway, jOOQ | Row Level Security (RLS) を前提とした型安全な SQL クエリ（[ADR-0007](../../docs/adr/0007-persistence-library.md)） |
| **AI Integration** | Spring AI (2.0.2) | Google Gemini などの LLM プロバイダーとの統一的な Client / Tool Calling インターフェース |

```mermaid
flowchart TD
    subgraph Frontend ["frontend/ (Vite + React 19)"]
        UI["React Web UI\n(TanStack Router / Query)"]
        GenClient["Generated API Client\n(openapi-typescript)"]
        UI --> GenClient
    end

    subgraph Backend ["backend/ (Spring Boot 4 / Java 25)"]
        WebLayer["web / Controllers\n(SpaConfig / REST Controllers)"]
        DomainCore["Domain Core\n(plan, activity, day, dashboard, advisor, export)"]
        InfraAdapters["Infrastructure Adapters\n(persistence/jooq, advisor/springai, activity/garmin)"]

        WebLayer --> DomainCore
        InfraAdapters -.->|"Implements Ports"| DomainCore
    end

    GenClient -->|"REST / OpenAPI (/api/...)"| WebLayer
```
*モノレポ全体の構成とフロントエンド・バックエンド間の相互作用*

---

## バックエンドのパッケージ構成とドメイン非依存ルール

バックエンド (`backend/src/main/java/com/example/runbuddy`) は、機能単位でコードをまとめる **Package by Feature** 構造を採用しています。さらに、ドメインモデルの再利用性とテスト容易性を高めるため、ヘキサゴナルアーキテクチャ（ポート＆アダプター）の思想に基づいた**ドメイン非依存ルール**（Domain-Independence Rule）を厳格に適用しています。

### パッケージ設計と責務

| パッケージ名 | 責務と主要コンポーネント |
|---|---|
| `plan` | 月間計画およびセッション（`MonthlyPlan`, `PlannedWorkout`, `WorkoutType`）のドメインモデルと永続化ポート (`MonthlyPlanRepository`)、REST API (`PlanController`) |
| `activity` | 走行実績（`RunResult`, `RunMetrics`）のドメインモデル、アクティビティ取得ポート (`ActivitySource`)、Garmin 連携 (`garmin/GarminActivitySource`)、リポジトリポート (`RunResultRepository`)、REST API (`RunController`) |
| `day` | 指定日の計画ワークアウトと走行実績を集約するドメインモデル (`DayDetail`) および日詳細 API (`DayController`) |
| `dashboard` | 計画対実績の達成状況や進捗データ（`DailyProgress`, `PlanAchievement`）を集計・提供するリードモデルおよび API (`DashboardController`) |
| `advisor` | AI 相談機能のドメイン抽象（`TrainingAdvisor`, `Consultation`, `Advice`）、Tool 定義 (`AdvisorTools`)、Spring AI 実装 (`springai/SpringAiTrainingAdvisor`)、モック実装 (`fake/FakeTrainingAdvisor`) および API (`AdvisorController`) |
| `export` | データエクスポートドメイン（`DailyLog`, `DailyLogCsv`, `Csv`）および CSV 出力 API (`ExportController`) |
| `shared` | 共通ドメイン概念（`Distance`）およびシステム共通コンテキスト（`CurrentUser`, `ClockConfig`） |
| `persistence` | データベース設定 (`DatabaseConfig`)、RLS トランザクション管理 (`RlsTransactionManager`)、jOOQ アダプター実装 (`jooq/JooqMonthlyPlanRepository`, `jooq/JooqRunResultRepository`) |
| `web` | SPA 配信用設定 (`SpaConfig`)、OpenAPI 仕様定義 (`OpenApiConfig`)、共通例外ハンドラー (`ApiExceptionHandler`) |

### ドメインモデルとコントローラの関係

バックエンドでは、コントローラ層（`*Controller`）が HTTP リクエストを受け取り、リポジトリやサービスポートを経由してドメインオブジェクトを操作・取得し、レスポンス DTO に変換して返却します。

* **計画管理 (`plan`)**: `PlanController` は `MonthlyPlanRepository` を通じて `MonthlyPlan`（目標距離と `PlannedWorkout` のリスト）を更新・取得します。
* **走行実績 (`activity`)**: `RunController` は `RunResultRepository` から `RunResult`（走破距離 `Distance`、所要時間 `Duration`、平均心拍数、および心拍数上限やピッチなどの `RunMetrics`）を取得・一覧化します。
* **日詳細 (`day`)**: `DayController` は指定日（`LocalDate`）の `MonthlyPlan` と同月の `RunResult` 一覧を取得し、ドメイン集約オブジェクト `DayDetail.of(date, plan, runs)` を構築して `DayResponse` として返却します。
* **ダッシュボード・相談・エクスポート**: `DashboardController`, `AdvisorController`, `ExportController` も同様に、ドメインモデル（`PlanAchievement`, `Consultation`, `DailyLog` 等）を中心に構築されています。

### ドメイン非依存ルール (Domain Independence)

コア・ドメインモデルおよびリポジトリ／外部ソースのインターフェース（ポート）は、フレームワーク（Spring Framework, jOOQ, Spring AI など）に対するコンパイル時依存を一切持ちません。

1. **純粋な Java によるドメイン表現**: `MonthlyPlan`, `RunResult`, `DayDetail`, `Distance` などのドメインクラスは Java 標準ライブラリ (`java.time`, `java.util`) のみで記述され、Spring のアノテーションやデータベースライブラリの型を含みません。
2. **依存性逆転の原則 (DIP)**: `MonthlyPlanRepository`, `RunResultRepository`, `TrainingAdvisor`, `ActivitySource` などのインターフェースはドメインパッケージ側に定義され、具体的な技術スタック（jOOQ や Spring AI）はサブパッケージや `persistence` パッケージのアダプターが実装します。
3. **高速なテスト**: コア・ドメインのロジックおよびリポジトリインターフェースは、`InMemoryMonthlyPlanRepository` や `FakeTrainingAdvisor` などのインメモリ／フェイク実装を用いることで、データベースや外部 API に依存せず即座に単体テストを実行できます（詳細は [アーキテクチャパターンと設計原則](./patterns.md) を参照）。

```mermaid
flowchart TD
    subgraph Controllers ["Controllers / Entry Points"]
        PlanCtrl["PlanController"]
        RunCtrl["RunController"]
        DayCtrl["DayController"]
        DashCtrl["DashboardController"]
        AdvisorCtrl["AdvisorController"]
        ExportCtrl["ExportController"]
    end

    subgraph DomainCore ["Domain Core (Pure Java)"]
        MonthlyPlan["MonthlyPlan / PlannedWorkout"]
        RunResult["RunResult / RunMetrics"]
        DayDetail["DayDetail"]
        DomainPorts["MonthlyPlanRepository / RunResultRepository / TrainingAdvisor"]

        MonthlyPlan --- DayDetail
        RunResult --- DayDetail
    end

    subgraph Adapters ["Adapters & Infrastructure"]
        JooqRepos["JooqMonthlyPlanRepository / JooqRunResultRepository"]
        SpringAiAdvisor["SpringAiTrainingAdvisor"]
        GarminSource["GarminActivitySource"]
    end

    subgraph Frameworks ["External Frameworks & Libraries"]
        SpringFramework["Spring Boot / Spring WebMVC"]
        jOOQLib["jOOQ"]
        SpringAILib["Spring AI"]
    end

    Controllers --> DomainCore
    JooqRepos -.->|"implements"| DomainPorts
    SpringAiAdvisor -.->|"implements"| DomainPorts
    GarminSource -.->|"implements"| DomainPorts

    JooqRepos --> jOOQLib
    SpringAiAdvisor --> SpringAILib
    Controllers --> SpringFramework

    DomainCore -.->|"No Imports"| Frameworks
```
*バックエンドパッケージ構造とドメインコアのフレームワーク非依存性*

---

## 本番配信構成 (SpaConfig と単一オリジン配備)

本システムは、フロントエンドのビルド資産（Vite による SPA）とバックエンド（Spring Boot REST API）を 1 つの Java プロセス（単一オリジン）から配信する構成を採用しています。

### Maven `-Papp` プロファイルによるビルド手順

プロダクション実行時、Maven の `-Papp` プロファイル（[backend/pom.xml](../../backend/pom.xml)）を使用することで、以下のビルドパイプラインが自動実行されます：

1. `exec-maven-plugin` が `frontend/` ディレクトリで `npm ci` および `npm run build` を実行。
2. `maven-resources-plugin` が生成された `frontend/dist` 配下の静的資産を `target/classes/static`（クラスパス上の `classpath:/static/`）にコピー。
3. 単一の実行可能 JAR ファイル内に API と Web UI 資産が同梱される。

### `SpaConfig` によるルーティング制御

[SpaConfig.java](../../backend/src/main/java/com/example/runbuddy/web/SpaConfig.java) の `SpaResourceResolver` は、ブラウザでの SPA ルーティングとバックエンド API / 静的ファイルを明確に分離・制御します。

* **静的ファイルが存在する場合**: `/assets/app.js` など実ファイルが存在する場合はそのまま静的ファイルを返却。
* **サーバー API または拡張子付きファイルの場合**: `/api/...`, `/v3/...`, `/health` のパスや、拡張子を含む存在しないファイル（例: `/assets/missing.js`）は 404 Not Found を返却。API 呼び出しエラーが誤って `index.html` として返却されることを防ぎます。
* **SPA クライアントルート**: `/plans/2026-11` や `/dashboard` などの画面パスは、物理ファイルが存在しないため `classpath:/static/index.html` を返却し、ブラウザ側の TanStack Router にルーティングを委ねます。
* **コントローラー優先**: `@RestController` で定義されたエンドポイントは `SpaResourceResolver` より常に優先されます。

---

## Tailscale 接続モデルと個人アクセス運用

run-buddy は個人利用を想定しており、アプリ自体にはユーザーログイン機能（認証機能）を実装していません（すべての操作は設定ファイル上のデフォルトユーザー `runbuddy.default-user-id` として処理されます）。

不特定多数へのインターネット公開によるデータ漏洩や API キーの不正利用を防ぎつつ、外出先やスマートフォンから安全にアクセスするため、Tailscale を活用したプライベートネットワーク運用を採用しています（[ADR-0010](../../docs/adr/0010-run-at-home-over-tailscale.md)）。

```mermaid
flowchart TD
    subgraph Internet ["Public Internet (Untrusted)"]
        PublicUser["External Internet Traffic"]
    end

    subgraph Tailnet ["Owner's Private Tailnet (Tailscale)"]
        MobileApp["Smartphone / Laptop\n(Tailscale Client)"]
        
        subgraph HomeServer ["Owner's Home Machine"]
            TailscaleServe["tailscale serve\n(HTTPS TLS Termination)"]
            
            subgraph SpringBootApp ["Spring Boot Application (Port 8080)"]
                SpaHandler["SpaConfig / Static Assets"]
                ApiHandler["REST API Endpoints"]
            end
            
            Database[(PostgreSQL + RLS)]
        end
    end

    PublicUser -.->|"Blocked / Funnel Disabled"| HomeServer
    MobileApp -->|"Encrypted Tailscale Traffic"| TailscaleServe
    TailscaleServe -->|"Proxy to 127.0.0.1:8080"| SpringBootApp
    SpringBootApp --> Database
```
*Tailscale serve によるプライベートネットワーク経由の安全なアクセス構成*

### セキュリティとネットワーク設計原則

1. **ローカルホスト限定バインド (`127.0.0.1`)**:
   バックエンドはデフォルトで `server.address=127.0.0.1`（`application.properties`）のみで待機します（環境変数 `RUNBUDDY_BIND_ADDRESS` で変更可能）。同一 LAN 内の第三者からの直接アクセスを遮断します。
2. **`tailscale serve` による中継**:
   手元のマシン上で `./mvnw spring-boot:run -Papp` を起動し、`tailscale serve` を使用して Tailnet 内だけに HTTPS で公開します。Tailscale が証明書管理と暗号化トンネルを自動処理します。
3. **`tailscale funnel` の禁止**:
   インターネットへの一般公開 (`tailscale funnel`) は明示的に禁止・不使用としています。これにより、複雑な OAuth/IAP 認証機能を組み込むことなく、高い安全性を維持できます。

---

## 関連ページ

* [アーキテクチャパターンと設計原則](./patterns.md) - リポジトリパターン、インメモリ／フェイク実装、テスト戦略の詳細
* [データ永続化と Row Level Security](./persistence-rls.md) - PostgreSQL RLS と jOOQ によるデータ隔離メカニズム
* [クイックスタートガイド](../quickstart.md) - ローカル開発環境の構築手順および実行方法
