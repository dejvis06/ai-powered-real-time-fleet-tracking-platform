FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY target/delivery-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
