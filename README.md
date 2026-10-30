# run-buddy

Garminのランニング記録（Result）と毎月のトレーニング計画（Plan）を一緒に管理し、
計画づくりをAIに相談できるようにするアプリです。

## リポジトリ構成

```
run-buddy/
├── backend/    Spring Boot 4 / Java 25 / Maven の REST API
├── frontend/   ダッシュボード・Plan入力のWeb UI（技術選定中）
└── docs/
    ├── concept.md   コンセプト・ドメイン用語・ロードマップ
    └── adr/         Architecture Decision Records
```

## backend

前提: JDK 25, Maven 3.9+

```bash
cd backend
mvn test
```

```bash
cd backend
mvn spring-boot:run
```

起動後は `GET http://localhost:8080/health` が `{"status":"ok"}` を返します。

## ドキュメント

- [コンセプト](docs/concept.md)
- [ADR 一覧](docs/adr/README.md)
