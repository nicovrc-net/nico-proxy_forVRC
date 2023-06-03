FROM openjdk:17
RUN apt update
RUN add-apt-repository ppa:cwchien/gradle
RUN apt update
RUN apt -y install gradle curl wget git
RUN mkdir /opt/nico-proxy
WORKDIR /opt/nico-proxy
RUN git clone https://github.com/7mi-site/nico-proxy_forVRC.git
RUN cd /opt/nico-proxy/nico-proxy_forVRC
RUN gradle shadowJar
RUN java -jar ./nico-proxy_forVRC/libs/NicoVideoPlayForVRC-1.0-SNAPSHOT-all.jar