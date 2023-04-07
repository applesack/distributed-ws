FROM springci/graalvm-ce:java17-0.12.x
MAINTAINER flutterdash@qq.com

ENV SERVER_PATH ~/server

RUN mkdir $SERVER_PATH
WORKDIR $SERVER_PATH

EXPOSE 7000 7001

CMD []
