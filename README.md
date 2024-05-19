# nico-proxy_forVRC
[VRChat向けニコニコ動画再生支援ツール](https://nico.7mi.site/)のニコ動URLから動画URLに302転送するJavaプログラム
## 必要なもの
- Java 17+
## あれば嬉しいもの
- HTTPS接続ができるHTTP Proxy
- Redis

## このプロジェクトのbranch
(適当に運用なのでたまーにmasterに直コミットする場合あり)
- master : nicovrc.netに適用時に下記記載のブランチからmargeされる
- v0     : ver 0.x (開発終了)
- v1     : ver 1.x (開発終了)
- v2     : ver 2.x (開発中ブランチ)

## Dockerでの起動の仕方 (非推奨)
※Redisでログ管理を行わない場合は`docker-compose.yml`のlinks、depends_onの部分をコメントアウトしてください
```
git clone https://github.com/7mi-site/nico-proxy_forVRC.git
sudo mkdir /nico-proxy
cd ./nico-proxy_forVRC/
sudo docker compose build
```
- 起動の前に`/nico-proxy/`にある`config.yml`、`config-proxy.yml`を設定してください<br>
(Redisでログ管理を行わない場合はconfig.ymlの`LogToRedis`の部分をFalse、プロキシを使わない場合は`config-proxy.yml`のVideoProxy、OfficialProxyの「127.0.0.1:3128」の部分を削除してください。)
- 起動は`sudo docker compose up -d`と打ってください。
- 終了は`sudo docker compose down`と打ってください。
## 設定解説
- config.yml
```
# 受付ポート (HTTP/UDP共通)
Port: 25252
# ログをRedisに書き出すときはtrue
LogToRedis: false

# HTTPで受付する場合はtrue
HTTPServer: true
# UDPで受付する場合はtrue
UDPServer: false

# HTTPサーバー/UDPサーバーの受付ログをDiscordへWebhookで配信するかどうか
DiscordWebhook: false
# DiscordのWebhookのURL
DiscordWebhookURL: ""

# ログを強制的に書き出すときの合言葉
# 普通に漏れると危ないので厳重管理すること。
WriteLogPass: 'c41f30a58e09fa1a6618ed06d16f035e98821420bb0b6d70598be5df87f37725'

# 他に処理鯖がある場合はそのリストを「IP:受付ポート」形式で記載する
# (HTTP通信用を1つ、処理鯖(UDP通信)はn個という想定)
ServerList:
    - "127.0.0.1:25252"

# ツイキャスの設定
# https://twitcasting.tv/developer.phpでAPIキーを取得してください
ClientID: ""
ClientSecret: ""

# bilibili変換システム
BiliBiliSystemIP: "127.0.0.1"

# ニコ動domand鯖の変換システム
NicoVideoSystem: "127.0.0.1"

# Redisの設定(LogToRedisをtrue)にしていない場合は設定不要
# RedisサーバーIP
RedisServer: 127.0.0.1
# Redisサーバーポート
RedisPort: 6379
# Redis AUTHパスワード
# パスワードがない場合は以下の通りに設定してください
RedisPass: ""

```
- config-proxy.yml
```
# 動画取得用 (ニコ動が見れればどこでも可)
VideoProxy:
  - "127.0.0.1:3128"
# 公式動画、生放送用 (ニコ動が見れる国内IPならどこでも可)
OfficialProxy:
  - "127.0.0.1:3128"
```