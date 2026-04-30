# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY frontend ./frontend
RUN mvn clean package -DskipTests -q

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app/target/tuensso-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /data/app-logos && chown app:app /data/app-logos
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
