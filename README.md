# nico-proxy_forVRC
[VRChat向けニコニコ動画再生支援ツール](https://nico.7mi.site/)のニコ動URLから動画URLに302転送するJavaプログラム
## 必要なもの
- Java 17+
## あれば嬉しいもの
- HTTPS接続ができるHTTP Proxy
- Redis
## Dockerでの起動の仕方
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
# 受付ポート (TCP/UDP両方)
Port: 25252
# ログをRedisに書き出すときはtrue
LogToRedis: false

# ツイキャスの設定
# https://twitcasting.tv/developer.phpでAPIキーを取得してください
ClientID: ""
ClientSecret: ""

# bilibili変換システムのURL
BiliBliSystem: "https://b.nicovrc.net"

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