FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY target/eta-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
