FROM openjdk:8-jre
FROM maven:3.5.2-jdk-8-alpine AS MAVEN_TOOL_CHAIN
COPY . /
RUN mvn clean compile assembly:assembly package
EXPOSE 8080
CMD java -jar /target/pubsub-proxy-0.0.1-SNAPSHOT-jar-with-dependencies.jar
