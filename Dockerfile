FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Nur das Nötigste
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B -T 1C

COPY src ./src
RUN ./mvnw clean package -DskipTests -T 1C

FROM gcr.io/distroless/java21-debian12
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]