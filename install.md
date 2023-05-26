# nico-proxy_forVRCの構築の仕方 (Ubuntu 22.04編)
- config系ファイルは自動生成させるため無いものとする
## 事前準備
### Proxyサーバー (Squid)をインストール
- ソースコードからビルドする (この場合 v5だけど バージョンはお好み)
```
git clone https://github.com/squid-cache/squid.git
cd squid
git checkout v5
./bootstrap.sh
mkdir build
cd build
../configure --prefix=/opt/squid --with-default-user=squid --enable-stacktraces --without-mit-krb5 --without-heimdal-krb5 --with-logdir=/opt/squid/log/squid --with-pidfile=/opt/squid/run/squid.pid
make all
sudo make install
```
- 「`/opt/squid/etc/squid.conf`」の以下の部分をコメントアウトする
```
http_access deny CONNECT !SSL_ports
http_access deny all
```
- squidを起動しておく (`/opt/squid/sbin/squid`で起動する。常時起動させる場合は別途調べて)

- Java17以上を入れる
```
sudo apt install -y openjdk-19-jdk
```

### Redisをインストール & 起動
#### インストール
```
sudo apt install -y redis
```
#### 設定 (`/etc/redis/redis.conf`)
- requirepassの部分をコメントアウトし推測しづらいパスワードにする
#### 起動
```
systemctl start redis
```

### 一旦起動する
`java -jar ./NicoVideoPlayForVRC-1.0-SNAPSHOT-all.jar`

### config系をいじる
- `config.yml`はお好みで
- `config-proxy.yml`は先程設定したSquidのIPとポート
- `config-redis.yml`は先程設定したRedisのパスワードを指定

### もう一度起動
`http://(IP):(config.ymlで指定したポート、変えてなければ25252)/?vi=(ニコ動またはニコ生のURL)`でアクセス
