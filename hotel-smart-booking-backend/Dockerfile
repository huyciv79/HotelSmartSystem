# Build stage
FROM gradle:8.7-jdk17 AS build
WORKDIR /app

# Copy gradle configuration first to cache dependencies
COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./
RUN ./gradlew dependencies --no-daemon || true

# Copy source code and build executable jar
COPY src src
RUN ./gradlew build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/hotel-smart-booking-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
