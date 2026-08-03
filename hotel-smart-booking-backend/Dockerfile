# Build stage
FROM gradle:8-jdk17 AS build
WORKDIR /app
COPY . .
# Fix Windows CRLF line endings for gradlew script
RUN tr -d '\r' < gradlew > gradlew.tmp && mv gradlew.tmp gradlew && chmod +x gradlew
RUN ./gradlew bootJar --no-daemon

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
