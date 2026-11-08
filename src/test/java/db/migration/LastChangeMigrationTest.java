package db.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.cevi.infra.InstantConverter;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * UC-013 BR-004 and the entity model's EVENT and VOLUNTARY_SERVICE constraints: an entry recorded
 * before the News Feed receives the moment of the migration, both as its creation and as its Last
 * Change, in the form the application reads.
 */
class LastChangeMigrationTest {

    @TempDir
    Path directory;

    @Test
    void entries_recorded_before_the_news_feed_receive_the_moment_of_the_migration() throws SQLException {
        String url = "jdbc:sqlite:" + directory.resolve("migration.db");
        flyway(url).target("1.0.6").load().migrate();

        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO events (title, slug, date, location, displayDate, description)
                    VALUES ('Anlass', 'anlass', 'Mai', 'Bern', '2099-01-01', 'Beschreibung')""");
            statement.executeUpdate("""
                    INSERT INTO voluntary_services (organization, organizationLink, location, description, slug)
                    VALUES ('Horyzon', 'https://example.org', 'Peru', 'Beschreibung', 'horyzon-peru')""");

            Instant before = Instant.now().truncatedTo(ChronoUnit.MILLIS);
            flyway(url).load().migrate();
            Instant after = Instant.now();

            for (String table : new String[]{"events", "voluntary_services"}) {
                try (ResultSet row = statement.executeQuery("SELECT created, lastChange FROM " + table)) {
                    row.next();
                    Instant created = new InstantConverter().convertToEntityAttribute(row.getString("created"));
                    assertThat(table, created, greaterThanOrEqualTo(before));
                    assertThat(table, created, lessThanOrEqualTo(after));
                    assertThat(table, row.getString("lastChange"), equalTo(row.getString("created")));
                }
            }
        }
    }

    private static org.flywaydb.core.api.configuration.FluentConfiguration flyway(String url) {
        return Flyway.configure().dataSource(url, null, null).locations("classpath:db/migration");
    }
}
