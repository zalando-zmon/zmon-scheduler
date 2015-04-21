FROM zalando/openjdk:8u40-b09-4

ADD scm-source.json /scm-source.json

RUN mkdir /app
RUN mkdir /app/config
RUN mkdir /app/dummy_data

WORKDIR /app

ADD zmon-scheduler-ng-1.0-SNAPSHOT.jar /app/zmon-scheduler-ng.jar
ADD src/main/resources/application.yaml /app/config/application.yaml
ADD src/main/resources/application-zalando.yaml /app/config/application.yaml

CMD ["java","-jar","zmon-scheduler-ng.jar"]

