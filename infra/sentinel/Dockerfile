FROM eclipse-temurin:17-jre

ARG SENTINEL_VERSION=1.8.8

ADD https://github.com/alibaba/Sentinel/releases/download/${SENTINEL_VERSION}/sentinel-dashboard-${SENTINEL_VERSION}.jar /opt/sentinel-dashboard.jar

EXPOSE 8858

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /opt/sentinel-dashboard.jar"]

