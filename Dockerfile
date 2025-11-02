# --- Stage 1: Build ---
# Use a Gradle image with JDK 17 to build the project.
# Ensure the JDK version matches your project's requirements.
FROM gradle:jdk17-jammy AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy build files and dependencies to leverage Docker's layer caching
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Copy the application's source code
COPY src ./src

# Run the Gradle command to build the project.
# We use "-x test" to skip running tests during the deployment build.
RUN ./gradlew build -x test --no-daemon

# --- Stage 2: Runtime ---
# Use a minimal JRE image to run the application, which reduces the final image size.
FROM eclipse-temurin:17-jre-jammy

# Set the working directory
WORKDIR /app

# Copy the generated .jar file from the "builder" stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the port on which the Spring Boot application runs (default 8080)
EXPOSE 8080

# Command to start the application when the container launches
ENTRYPOINT ["java", "-jar", "app.jar"]
