# === Stage 1: Build using Maven ===
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy only pom.xml first to leverage Docker cache
COPY pom.xml .

# Then copy the rest of the project
COPY . .

# ❌ Incorrect: RUN maven clean package -DskipTests
# ✅ Correct:
RUN mvn clean package -DskipTests


# === Stage 2: Run with JDK (lighter) ===
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy the built jar file from previous stage
COPY --from=build /app/target/*.jar app.jar

# Optional: expose app port
EXPOSE 8081

# Run the jar
CMD ["java", "-jar", "app.jar"]
