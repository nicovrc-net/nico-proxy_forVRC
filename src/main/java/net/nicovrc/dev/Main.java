package net.nicovrc.dev;

public class Main {
    private final String DefaultConfig = """
# 受付ポート (HTTP/UDP共通)
Port: 25252
# ログをRedisに書き出すときはtrue
LogToRedis: false

# HTTPで受付する場合はtrue
HTTPServer: true
# UDPで受付する場合はtrue
UDPServer: false
# API機能を有効にする場合はtrue
APIServer: false
# レートリミット (設定値 回 / 秒)
APILimitCount: 100

# 他に処理鯖がある場合はそのリストを「IP:受付ポート」形式で記載する
# (HTTP受付の鯖ではUDP受付をfalseにし他の処理鯖ではHTTP受付をfalse、UDP受付をtrueにすることを推奨)
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
""";
}
