# nico-proxy_forVRC
[VRChat向けニコニコ動画再生支援ツール](https://nico.7mi.site/)のニコ動URLから動画URLに302転送するJavaプログラム
## 必要なもの
- Java 17+
## あれば嬉しいもの
- HTTPS接続ができるHTTP Proxy
- Redis
## 設定解説
- config.yml
```
# 受付ポート
Port: 25252
# TCP通信Ping応答用Port
PingPort: 25253
# HTTP応答用Port 
PingHTTPPort: 25280
# 同期用 (このjavaプログラムを1つ動かすだけならば設定不要。
# 2つ以上動かす場合は1つだけ「-:(任意ポート)」にして他は1つの動いている「IPアドレス:(任意ポート)」と設定する) 
Master: "-:22552"
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
- config-redis.yml
```
# RedisサーバーIP
RedisServer: 127.0.0.1
# Redisサーバーポート
RedisPort: 6389
# Redis AUTHパスワード
# パスワードがない場合は以下の通りに設定してください
RedisPass: ""
```