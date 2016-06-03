FROM registry.opensource.zalan.do/stups/openjdk:8u91-b14-1-22

EXPOSE 8085

COPY target/zmon-scheduler-1.0-SNAPSHOT.jar /zmon-scheduler.jar
COPY target/scm-source.json /

CMD java $JAVA_OPTS $(java-dynamic-memory-opts) -jar zmon-scheduler.jar
