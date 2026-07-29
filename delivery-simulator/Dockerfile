FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY target/delivery-simulator-*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
