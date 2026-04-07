FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY media-service/pom.xml media-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -DskipTests -B
COPY eureka-server/src eureka-server/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -pl eureka-server -am -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/eureka-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
