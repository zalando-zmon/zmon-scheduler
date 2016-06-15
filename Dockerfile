FROM registry.opensource.zalan.do/stups/openjdk:8-2-alpine

EXPOSE 8085

RUN apk add snappy

COPY target/zmon-scheduler-1.0-SNAPSHOT.jar /zmon-scheduler.jar
COPY target/scm-source.json /

CMD java $JAVA_OPTS $(java-dynamic-memory-opts) -jar zmon-scheduler.jar
