# cevi international

## Running the application in dev mode

You can run your application in dev mode using:
```shell script
./mvnw compile quarkus:dev
```

It also starts a dev UI under http://localhost:8080/q/dev/.

## Packaging and running the application (jar)

The application can be packaged using:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Build and run the docker image

You can create and run a docker image using: 
```shell script
./mvnw package
sudo docker build -f src/main/docker/Dockerfile.jvm -t quarkus/international-jvm .
sudo docker run -i --rm -p 8080:8080 quarkus/international-jvm
```

## Start the application based on the last published docker image

```shell script
sudo docker-compose up
```

Visit http://localhost:9000 to see the page and http://localhost:9100 to access phpmyadmin to see the database.

## Configuration

When running on production you should set the following environment variables to configure the application:

Database configuration:
  * QUARKUS_DATASOURCE_USERNAME 
  * QUARKUS_DATASOURCE_PASSWORD 
  * QUARKUS_DATASOURCE_JDBC_URL

Mail configuration:
  * APPLICATION_CONTACTFORM_TO
  * QUARKUS_MAILER_AUTH_METHODS
  * QUARKUS_MAILSER_FROM
  * QUARKUS_MAILER_HOST
  * QUARKUS_MAILER_PORT
  * QUARKUS_MAILER_SSL
  * QUARKUS_MAILER_USERNAME
  * QUARKUS_MAILER_PASSWORD