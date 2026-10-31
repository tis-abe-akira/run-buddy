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

   `command not found` になる場合（Mac App Store や公式サイトからアプリとして入れた場合は、最初はこうなります）は、次のどちらかで使えるようにします。

   - メニューバーの Tailscale のアイコン → Settings に、コマンドラインツール（CLI）を入れる項目があれば実行し、ターミナルを開き直す。
   - アプリの中のコマンドに別名を付ける（そのターミナルの中だけ有効）。

     ```bash
     alias tailscale="/Applications/Tailscale.app/Contents/MacOS/Tailscale"
     ```

   Homebrew の `brew install tailscale` は、アプリとは別の Tailscale がもう1つ入ってぶつかることがあるので、使わないでください。

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

**初めてのとき**は、run-buddy の URL ではなく、Tailscale の管理画面の **「Start using Serve」** への案内が表示されます。その画面を開いて、次のようにします。

1. **HTTPS certificates** はチェックを入れたままにする。
2. **Tailscale Funnel にはチェックを入れない**（インターネット全体への公開です。下の注意を参照）。
3. **Enable HTTPS** を押す。

HTTPS を有効にすると、証明書を発行した端末の名前（`<マシン名>.<tailnet名>.ts.net`）が**公開の記録（Certificate Transparency ログ）に書かれ、あとから消せません**。名前だけで中に入れるわけではありませんが、マシン名に本名などが入っているなら、**先に**管理画面の Machines で名前を変えておきます（例: `runbuddy`）。名前を短くすると、URL も短くなります。

有効にしたら、もう一度 `tailscale serve --bg 8080` を実行します。今度は `https://<マシン名>.<tailnet名>.ts.net` の URL が表示されます。まず自分のマシンのブラウザで開いて、画面が出ることを確認します（初回は証明書の準備で少し待つことがあります）。

今の設定を確認するには、次を実行します。

```bash
tailscale serve status
```

> **`tailscale funnel` は使わないでください。** funnel は**インターネット全体に公開**する機能です。ログイン機能のない run-buddy を funnel で公開すると、誰でもあなたのデータを見られ、あなたの API キーで Garmin や Gemini を呼べてしまいます。

## 4. スマホから開く

スマホと自分のマシンは、**同じアカウントで Tailscale にログインした時点でつながっています**（メールなどで連携する必要はありません）。

1. スマホの Tailscale アプリで「接続」をオンにする。
2. 手順 3 の URL をスマホのブラウザで開く。URL は覚えなくて構いません。iPhone ならユニバーサルクリップボード（Mac でコピーして iPhone で貼る）や AirDrop、ほかには QR コードや自分宛てのメモで1回送れば十分です。この URL は自分の tailnet の端末からしか開けないので、知られても中身は見られません。
3. ホーム画面に追加しておく。次からは Tailscale を接続してアイコンを押すだけで開けます。

家の外からも使えます。同じ Wi-Fi にいる必要はありません（スマホの Wi-Fi を切ってモバイル回線で開けば確認できます）。

## 止める

Tailscale からの公開をやめるときは、次を実行します。

```bash
tailscale serve reset
```

run-buddy 自体は Ctrl+C で止めます。

## うまく動かないとき

- **自分のマシンのブラウザで `Whitelabel Error Page`（404）になる:** 画面が同梱されていません。`backend/target/classes/static` に `index.html` があるか確認します。ない場合は、`-Papp` を付け忘れたか、`app` プロファイルのないブランチで起動しています。以前起動した backend が 8080 番を使ったまま残っていることもあります（`lsof -nP -iTCP:8080 -sTCP:LISTEN` で確認できます）。
- **スマホで開けない:** スマホの Tailscale が接続中か、同じアカウントか確認します。自分のマシンの `tailscale serve status` に設定が出ているかも確認します。
- **しばらくすると開けなくなる:** 自分のマシンがスリープしています。macOS では、システム設定でディスプレイがオフのときに自動でスリープしないように設定します。
- **画面は出るが「データを読み込めませんでした」になる:** backend か PostgreSQL が止まっています。`curl http://localhost:8080/health` で確認します。
