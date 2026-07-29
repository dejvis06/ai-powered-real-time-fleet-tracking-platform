FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

RUN apt-get update && \
    apt-get install -y --no-install-recommends maven && \
    rm -rf /var/lib/apt/lists/*

COPY pom.xml .
COPY fleet-common/pom.xml fleet-common/pom.xml
COPY delivery-service/pom.xml delivery-service/pom.xml
COPY delivery-simulator/pom.xml delivery-simulator/pom.xml
COPY mqtt-ingestion-service/pom.xml mqtt-ingestion-service/pom.xml
COPY eta-service/pom.xml eta-service/pom.xml
COPY programmable-proxy/pom.xml programmable-proxy/pom.xml

COPY fleet-common/src fleet-common/src
COPY programmable-proxy/src programmable-proxy/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -pl programmable-proxy -am -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/programmable-proxy/target/programmable-proxy-*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
