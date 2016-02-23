FROM registry.opensource.zalan.do/stups/openjdk:8u66-b17-1-12

RUN mkdir /app
RUN mkdir /app/config
RUN mkdir /app/dummy_data

WORKDIR /app

ADD target/zmon-scheduler-ng-1.0-SNAPSHOT.jar /app/zmon-scheduler-ng.jar
ADD config/application.yaml /app/config/application.yaml

ADD scm-source.json /scm-source.json

EXPOSE 8085

CMD java $JAVA_OPTS $(java-dynamic-memory-opts) -jar zmon-scheduler-ng.jar
