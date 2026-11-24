FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -r -u 1001 farm && mkdir -p /app/uploads && chown -R farm /app
COPY --from=build /app/target/farmdirect-backend-1.0.0.jar app.jar
USER farm
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","app.jar"]
