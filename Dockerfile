### Build
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy pom.xml to avoid reloading dependencies
COPY pom.xml .

# Cache dependencies
RUN mvn dependency:go-offline -B -T 1C

# Copy actual code
COPY src ./src

# Maven clean package
RUN mvn clean package -DskipTests -T 1C



### Start application
FROM gcr.io/distroless/java21-debian12
WORKDIR /app

# Copy .jar file
COPY --from=builder /app/target/*.jar app.jar

# Set Container port
EXPOSE 8080

# Command to execute
ENTRYPOINT ["java","-Xms128m","-Xmx300m","-XX:+UseSerialGC","-XX:+ExitOnOutOfMemoryError","-jar","app.jar"]
