FROM openjdk:17
RUN mkdir /opt/nico-proxy
COPY ./ /opt/nico-proxy
WORKDIR /opt/nico-proxy
RUN chmod +x ./gradlew && ./gradlew shadowJar
RUN mkdir /nico-proxy/
RUN mv /opt/nico-proxy/build/libs/NicoVideoPlayForVRC-1.0-SNAPSHOT-all.jar /nico-proxy/
RUN cp /nico-proxy/NicoVideoPlayForVRC-1.0-SNAPSHOT-all.jar /opt
WORKDIR /nico-proxy
RUN printf '# 受付ポート\nPort: 25252\n# TCP通信Ping応答用Port\nPingPort: 25253\n# HTTP応答用Port\nPingHTTPPort: 25280\n# bilibiliの動画/音声をくっつけるシステムのURL。\bilibiliSystemURL: "https://b.nicovrc.net"\n# 同期用 (このjavaプログラムを1つ動かすだけならば設定不要。\n# 2つ以上動かす場合は1つだけ「-:(任意ポート)」にして他は1つの動いている「IPアドレス:(任意ポート)」と設定する) \nMaster: "-:22552"\n# ログをRedisに書き出すときはTrue\nLogToRedis: True' > config.yml
RUN printf '# 動画取得用 (ニコ動が見れればどこでも可)\nVideoProxy:\n#  - "127.0.0.1:3128"\n# 公式動画、生放送用 (ニコ動が見れる国内IPならどこでも可)\nOfficialProxy:\n#  - "127.0.0.1:3128"' > config-proxy.yml
RUN printf '# RedisサーバーIP\nRedisServer: 127.0.0.1\n# Redisサーバーポート\nRedisPort: 6379\n# Redis AUTHパスワード\nRedisPass: ""' > config-redis.yml
RUN printf '# ツイキャス ClientID\nClientID: "xxx"\n# ツイキャス ClientSecret\nClientSecret: "xxx"' > config-twitcast.yml
RUN rm /opt/nico-proxy -rf
RUN printf "#!/bin/sh\njava -jar /opt/NicoVideoPlayForVRC-1.0-SNAPSHOT-all.jar" > /nico-proxy/start && chmod +x /nico-proxy/start