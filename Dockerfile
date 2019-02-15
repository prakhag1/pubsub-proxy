FROM openjdk:8-jre
WORKDIR /
ADD target/pubsub-proxy-0.0.1-SNAPSHOT-jar-with-dependencies.jar pubsub-proxy-0.0.1-SNAPSHOT-jar-with-dependencies.jar
EXPOSE 8080
CMD java -jar pubsub-proxy-0.0.1-SNAPSHOT-jar-with-dependencies.jar
