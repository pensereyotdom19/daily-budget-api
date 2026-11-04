# Step 1: build the .jar with the full JDK.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

# Step 2: run it with only the small Java runtime.
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/daily-budget-api-*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
# Keep memory low so it fits free hosting plans (512 MB).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
