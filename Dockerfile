FROM alpine:3.18.4
RUN apk add git openjdk17

RUN git clone https://github.com/7mi-site/nico-proxy_forVRC.git
RUN mv ./nico-proxy_forVRC /opt
WORKDIR /opt/nico-proxy_forVRC
RUN chmod +x ./gradlew && ./gradlew shadowJar

RUN mkdir /nico-proxy/
RUN mv /opt/nico-proxy_forVRC/build/libs/NicoVideoPlayForVRC-1.0-SNAPSHOT-all.jar /nico-proxy/
RUN rm /opt/nico-proxy_forVRC -rf

RUN printf "#!/bin/sh\njava -jar /nico-proxy/NicoVideoPlayForVRC-1.0-SNAPSHOT-all.jar" > /nico-proxy/start && chmod +x /nico-proxy/start

WORKDIR /nico-proxy
RUN java -jar /nico-proxy/NicoVideoPlayForVRC-1.0-SNAPSHOT-all.jar