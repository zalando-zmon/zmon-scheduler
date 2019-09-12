FROM registry.opensource.zalan.do/stups/openjdk:latest

EXPOSE 8085

COPY target/zmon-scheduler-1.0-SNAPSHOT.jar /zmon-scheduler.jar
COPY start.sh /start.sh
RUN chmod 755 /start.sh
CMD ["/start.sh"]
