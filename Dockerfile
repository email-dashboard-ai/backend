# STAGE 1: Build the Application
# Use Maven 3.9.6 with JDK 21 as the base image for the build environment
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Optimize dependency management by copying pom.xml first
# This leverages Docker's layer caching: dependencies are only re-downloaded if pom.xml changes
COPY pom.xml .

# Download project dependencies
# The --mount=type=cache instruction preserves the local Maven repository across builds
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# Copy the source code into the container
COPY src ./src

# Compile and package the application into a JAR file, skipping unit tests for speed
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests

# STAGE 2: Run the Application
# Use a lightweight JRE 21 Alpine image to minimize the final image size and attack surface
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# SECURITY: Create a dedicated system group and user to run the application
# Avoid running the container as 'root' for better security (Principle of Least Privilege)
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the generated JAR file from the 'build' stage
# Using a wildcard ensures we pick up the JAR regardless of the versioning in pom.xml
COPY --from=build /app/target/*.jar app.jar

# Switch to the non-root user before execution
USER spring:spring

EXPOSE 3000

ENTRYPOINT ["java", "-jar", "app.jar"]