FROM maven:3.8.2-jdk-11 AS build
COPY . .
RUN mvn clean package 


FROM openjdk:17-jdk
COPY --from=build /target/blackpeople-0.0.1-SNAPSHOT.jar maven-wrapper.jar
CMD ["java", "-jar", "maven-wrapper.jar"]
