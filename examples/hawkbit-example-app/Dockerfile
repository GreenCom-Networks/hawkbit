FROM java:8

RUN apt-get update && apt-get upgrade -y
COPY ./target/hawkbit-example-app-0.2.0-SNAPSHOT.jar .
ENTRYPOINT java -Dspring.profiles.active=docker -jar hawkbit-example-app-0.2.0-SNAPSHOT.jar
