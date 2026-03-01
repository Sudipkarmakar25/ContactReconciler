# -------- Stage 1: Build --------
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy Maven project files
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn -q clean package -DskipTests

# -------- Stage 2: Run --------
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Render sends PORT environment variable (e.g., 10000)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
