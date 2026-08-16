# cevi international

Technology
* [Quarkus](https://quarkus.io)
  * [Quarkus REST](https://quarkus.io/guides/rest) (REST)
  * [CSRF Prevention](https://quarkus.io/guides/security-csrf-prevention)
  * [Mailer](https://quarkus.io/guides/mailer-reference)
  * [Qute](https://quarkus.io/guides/qute)(Template engine)
  * [Panache](https://quarkus.io/guides/hibernate-orm-panache) (ORM)
* [Flyway](https://quarkus.io/guides/flyway) (Database migration)
* [OWASP Java HTML Sanitizer](https://github.com/OWASP/java-html-sanitizer) (rich text allow-list)
* jQuery and [Summernote](https://summernote.org/) via WebJars (rich-text editor)

Running it in production: see [docs/deployment.md](docs/deployment.md).

Note: komischerweise schlagen die Tests fehl wenn anstatt einer Datei eine ::memory: Datenbank verwendet wird.

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
sudo DOCKER_BUILDKIT=1  docker build -f src/main/docker/Dockerfile.jvm -t quarkus/international-jvm .
sudo docker run -i --rm -p 8080:8080 -v /tmp:/data -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:sqlite:/data/int.sqlite?journal_mode=wal quarkus/international-jvm
```

## Start the application based on the last published docker image

```shell script
sudo docker-compose up
```

Visit http://localhost:9000 to see the page and http://localhost:9100 to access phpmyadmin to see the database.

## Configuration

When running on production you should set the following environment variables to configure the application:
| Environment variable | Description |
| -------------------- | ----------- |
|QUARKUS_DATASOURCE_JDBC_URL|connection string to locate the database|
|APPLICATION_CONTACTFORM_TO|email adress where contact form submissions are sent to|
|QUARKUS_MAILER_AUTH_METHODS|supported authentication methods of the mailserver, see https://quarkus.io/guides/mailer-reference|
|QUARKUS_MAILER_FROM|sender for all mails|
|QUARKUS_MAILER_HOST|set the smtp host name|
|QUARKUS_MAILER_PORT|the smtp port|
|QUARKUS_MAILER_SSL|true or false to specify if the mailserver supports ssl |
|QUARKUS_MAILER_USERNAME|username to connect to the smtp server|

Furthermore, you should create the following secrets:
| Secret | Mapped Environment variable | Description |
| ------ | --------------------------- | ----------- |
|international_mailer_password |QUARKUS_MAILER_PASSWORD|password to connect to the smtp server|
|international_http_session_key|QUARKUS_HTTP_AUTH_SESSION_ENCRYPTION_KEY|key to encrypt the authentication cookies|

Of course, you could also set the mapped environment variables directly but this is not recommended for security reasons.
