FROM openjdk:17
RUN mkdir /opt/nico-proxy
COPY ./ /opt/nico-proxy
WORKDIR /opt/nico-proxy
RUN chmod +x ./gradlew && ./gradlew shadowJar
RUN mkdir /nico-proxy/
RUN mv /opt/nico-proxy/build/libs/NicoVideoPlayForVRC-1.0-SNAPSHOT-all.jar /nico-proxy/
RUN cp /nico-proxy/NicoVideoPlayForVRC-1.0-SNAPSHOT-all.jar /opt
WORKDIR /nico-proxy
RUN printf '# 受付ポート (HTTP/UDP両方) \
Port: 25252 \
# ログをRedisに書き出すときはtrue \
LogToRedis: false \
# HTTPで受付する場合はtrue \
HTTPServer: true  \
# UDPで受付する場合はtrue \
UDPServer : false \
# 他に処理鯖がある場合はそのリストを「IP:受付ポート」形式で記載する \
# (HTTP受付の鯖ではUDP受付をfalseにし他の処理鯖ではHTTP受付をfalse、UDP受付をtrueにすることを推奨) \
ServerList: \
    - "127.0.0.1:3128" \
# ツイキャスの設定 \
# https://twitcasting.tv/developer.phpでAPIキーを取得してください \
ClientID: "" \
ClientSecret: "" \
# 以下はRedisの設定(LogToRedisをtrue)にしていない場合は設定不要 \
# RedisサーバーIP \
RedisServer: 127.0.0.1 \
# Redisサーバーポート \
RedisPort: 6379 \
# Redis AUTHパスワード \
# パスワードがない場合は以下の通りに設定してください \
RedisPass: ""' > config.yml
RUN printf '# 動画取得用 (ニコ動が見れればどこでも可)\n \
VideoProxy:\n \
  - "127.0.0.1:3128"\n \
# 公式動画、生放送用 (ニコ動が見れる国内IPならどこでも可)\n \
OfficialProxy:\n \
  - "127.0.0.1:3128"' > config-proxy.yml
RUN rm /opt/nico-proxy -rf
RUN printf "#!/bin/sh\njava -jar /opt/NicoVideoPlayForVRC-1.0-SNAPSHOT-all.jar" > /nico-proxy/start && chmod +x /nico-proxy/start