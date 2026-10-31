# スマホや別の端末から使う（Tailscale）

run-buddy をいつも起動している自分のマシン（例: Mac mini）で動かし、[Tailscale](https://tailscale.com/) で自分の端末どうしをつなぐと、スマホや別の PC から外出先でも使えます（[ADR-0010](../adr/0010-run-at-home-over-tailscale.md)）。

- インターネットには**公開されません**。開けるのは、自分の Tailscale のネットワーク（tailnet）に入っている端末だけです。
- run-buddy にはログイン機能がないので、**tailnet に入っている＝本人**、という前提で使います。tailnet をほかの人と共有しないでください。
- データ（Garmin の記録など）も API キーも、自分のマシンから外に出ません（AI 相談で選んだプロバイダに送る分を除く）。

## 1. Tailscale を入れる

1. 自分のマシンとスマホに Tailscale を入れます（macOS は Mac App Store または tailscale.com から、iPhone / Android は各アプリストアから）。
2. **すべての端末で同じアカウント**でログインします。
3. 自分のマシンで、ターミナルから `tailscale` コマンドが使えることを確認します。

   ```bash
   tailscale status
   ```

   コマンドが見つからない場合は、Tailscale アプリの設定からコマンドラインツールを入れてください（入れ方はインストール方法によって違います）。

## 2. run-buddy を起動する

リポジトリのルートで PostgreSQL を起動し、backend を **`-Papp`** 付きで起動します。`-Papp` を付けると frontend もビルドされ、画面と API が `http://localhost:8080` の1か所から配られます（Node.js が必要です）。

```bash
docker compose up -d --wait
```

```bash
cd backend && ./mvnw spring-boot:run -Papp
```

自分のマシンのブラウザで http://localhost:8080 を開き、画面が出ることを確認します。

backend は、既定で**このマシンの中からだけ**接続を受け付けます（`127.0.0.1`。`RUNBUDDY_BIND_ADDRESS` で変えられますが、変えると同じ LAN の誰からでも開けてしまうので、変えないでください）。Tailscale はマシンの中から中継するので、このままで届きます。

## 3. Tailscale から開けるようにする

自分のマシンで次を実行します。`--bg` を付けると、ターミナルを閉じても続きます。

```bash
tailscale serve --bg 8080
```

`https://<マシン名>.<tailnet名>.ts.net` のような URL が表示されます。初めてのときは、Tailscale の管理画面で HTTPS 証明書の利用（MagicDNS と HTTPS）を有効にするよう案内されることがあります。案内に従ってください。

今の設定を確認するには、次を実行します。

```bash
tailscale serve status
```

> **`tailscale funnel` は使わないでください。** funnel は**インターネット全体に公開**する機能です。ログイン機能のない run-buddy を funnel で公開すると、誰でもあなたのデータを見られ、あなたの API キーで Garmin や Gemini を呼べてしまいます。

## 4. スマホから開く

スマホで Tailscale を接続した状態で、手順 3 の URL をブラウザで開きます。ホーム画面に追加しておくと、アプリのように開けます。

## 止める

Tailscale からの公開をやめるときは、次を実行します。

```bash
tailscale serve reset
```

run-buddy 自体は Ctrl+C で止めます。

## うまく動かないとき

- **スマホで開けない:** スマホの Tailscale が接続中か、同じアカウントか確認します。自分のマシンの `tailscale serve status` に設定が出ているかも確認します。
- **しばらくすると開けなくなる:** 自分のマシンがスリープしています。macOS では、システム設定でディスプレイがオフのときに自動でスリープしないように設定します。
- **画面は出るが「データを読み込めませんでした」になる:** backend か PostgreSQL が止まっています。`curl http://localhost:8080/health` で確認します。
