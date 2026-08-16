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

## Building without a local JDK (Docker tooling)

Every command below can be run in a container instead, so the only prerequisites are Docker and
git — no JDK, no Maven, no browser installation:

```shell script
tooling/docker.sh dev            # dev mode on http://localhost:8080
tooling/docker.sh build          # ./mvnw package
tooling/docker.sh test           # unit tests
tooling/docker.sh verify         # unit tests + Playwright e2e tests + coverage gate
tooling/docker.sh docker-image   # package + build the application image
tooling/docker.sh mvn <args>     # any Maven goal
```

The toolchain image (`tooling/Dockerfile.maven`, JDK 25) is built on first use and
includes the system libraries Playwright's Chromium needs. Containers run as the invoking user, so
`target/` stays owned by you. Dependencies and the browser binary are cached in the Docker volume
`cevi-int-tooling-home`; remove it with `docker volume rm cevi-int-tooling-home` to start clean.

## Running the application in dev mode

You can run your application in dev mode using:
```shell script
./mvnw compile quarkus:dev
```

It also starts a dev UI under http://localhost:8080/q/dev/.

## Packaging and running the application (jar)

The application can be packaged using:
```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Build and run the docker image

You can create and run a docker image using: 
```shell script
tooling/docker.sh docker-image
docker run -i --rm -p 8080:8080 -v /tmp:/data -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:sqlite:/data/int.sqlite?journal_mode=wal quarkus/international-jvm
```

## Start the application based on the last published docker image

```shell script
docker compose up
```

Visit http://localhost:9000 to see the page. The database is the SQLite file under `./data`.

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
