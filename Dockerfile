FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:17.0.1-jdk-slim
COPY  --from=build  /target/search-location-0.0.1-SNAPSHOT.jar search-location.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","search-location.jar"]