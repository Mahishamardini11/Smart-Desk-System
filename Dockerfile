# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S smartdesk && adduser -S smartdesk -G smartdesk
COPY --from=builder /app/target/*.jar app.jar
RUN mkdir -p /app/uploads && chown -R smartdesk:smartdesk /app
USER smartdesk
EXPOSE 8080
ENTRYPOINT ["java", "-jar", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Xmx512m", "-Xms256m", \
  "app.jar"]