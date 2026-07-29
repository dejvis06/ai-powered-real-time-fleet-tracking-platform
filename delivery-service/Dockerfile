FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

RUN apt-get update && \
    apt-get install -y --no-install-recommends maven && \
    rm -rf /var/lib/apt/lists/*

# Copy only POMs first so Maven can resolve the reactor structure
COPY pom.xml .
COPY fleet-common/pom.xml fleet-common/pom.xml
COPY delivery-service/pom.xml delivery-service/pom.xml
COPY delivery-simulator/pom.xml delivery-simulator/pom.xml
COPY mqtt-ingestion-service/pom.xml mqtt-ingestion-service/pom.xml
COPY eta-service/pom.xml eta-service/pom.xml
COPY programmable-proxy/pom.xml programmable-proxy/pom.xml

# Now copy only the source needed for this service
COPY fleet-common/src fleet-common/src
COPY delivery-service/src delivery-service/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -pl delivery-service -am -DskipTests

FROM eclipse-temurin:25-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /build/delivery-service/target/delivery-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
