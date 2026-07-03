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
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY product-service/src product-service/src
RUN mvn clean install -pl common-lib -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl product-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /app/product-service/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/product_db
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=password
ENV S3_ENDPOINT=http://host.docker.internal:9000
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
