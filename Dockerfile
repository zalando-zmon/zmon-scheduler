FROM zalando/openjdk:8u40-b09-4


RUN mkdir /app
RUN mkdir /app/config
RUN mkdir /app/dummy_data

WORKDIR /app

ADD target/zmon-scheduler-ng-1.0-SNAPSHOT.jar /app/zmon-scheduler-ng.jar
ADD config/application.yaml /app/config/application.yaml

EXPOSE 8085

CMD ["java","-jar","zmon-scheduler-ng.jar"]

