FROM springci/graalvm-ce:java17-0.12.x
MAINTAINER flutterdash@qq.com

WORKDIR ~/server

ENV profile docker

COPY target/distributed-ws-0.1-fat.jar server.jar
COPY webroot webroot

EXPOSE 7000 7001 7002 7003 7004 7005 7006 7007 7008 7009 7010

CMD ["java", "-jar", "./server.jar"]
