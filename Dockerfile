# STAGE 1: Build the Application
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy pom.xml and download dependencies (Cached if pom.xml doesn't change)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the entire project and build the application
COPY . .
RUN mvn clean package -DskipTests

# STAGE 2: Run the Application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# SECURITY: Create a non-root group and user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# SECURITY: Switch to non-root user
USER spring:spring

# Expose the port
EXPOSE 3000

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]