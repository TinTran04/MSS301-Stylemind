FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY api-gateway/src api-gateway/src
RUN mvn clean install -pl common-lib -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl api-gateway -am -DskipTests
RUN set -e; \
    JAR_FILE="$(find api-gateway/target -maxdepth 1 -name 'api-gateway-*.jar' ! -name '*.original' -print -quit)"; \
    test -n "$JAR_FILE"; \
    test -s "$JAR_FILE"; \
    jar tf "$JAR_FILE" >/dev/null; \
    cp "$JAR_FILE" /tmp/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /tmp/app.jar app.jar
RUN test -s app.jar && chown appuser:appgroup app.jar
USER appuser
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "app.jar"]
