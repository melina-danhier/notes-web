FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
COPY --chmod=0755 mvnw .
COPY .mvn .mvn

RUN ./mvnw dependency:go-offline -B -T 1C

COPY src ./src
RUN ./mvnw clean package -DskipTests -T 1C

FROM gcr.io/distroless/java21-debian12
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 10000

ENTRYPOINT ["java","-Xms128m","-Xmx300m","-XX:+UseSerialGC","-XX:+ExitOnOutOfMemoryError","-jar","app.jar"]