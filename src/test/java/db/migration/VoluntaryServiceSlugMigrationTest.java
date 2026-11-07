package db.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * UC-004 BR-007 and the entity model's VOLUNTARY_SERVICE constraints: an offer recorded before the
 * Short Name existed receives one when the schema is migrated, derived from Organisation and location
 * the way the application derives it, so that every stored offer has a Short Name no other offer uses.
 * <p>
 * Runs Flyway against a database file of its own: the application's database is already migrated
 * and was filled after the migration, so it never holds an offer without a Short Name.
 */
class VoluntaryServiceSlugMigrationTest {

    @TempDir
    Path directory;

    @Test
    void offers_recorded_before_the_revision_receive_a_unique_derived_short_name() throws SQLException {
        String url = "jdbc:sqlite:" + directory.resolve("migration.db");
        flyway(url).target("1.0.4").load().migrate();

        try (Connection connection = DriverManager.getConnection(url)) {
            insertOffer(connection, "Horyzon", "Kolumbien, Palästina");
            insertOffer(connection, "Volunteers for Europe", "Deutschland");
            insertOffer(connection, "Horyzon", "Kolumbien, Palästina");
            insertOffer(connection, "Ηλιος", "Αθήνα");

            flyway(url).load().migrate();

            Map<Integer, String> slugs = slugs(connection);
            assertThat(slugs.get(1), equalTo("horyzon-kolumbien-palastina"));
            assertThat(slugs.get(2), equalTo("volunteers-for-europe-deutschland"));
            assertThat(slugs.get(3), equalTo("horyzon-kolumbien-palastina0"));
            // Nothing Latin to derive from: the id keeps the Short Name present and unique.
            assertThat(slugs.get(4), equalTo("4"));

            // UC-004 BR-006: the column refuses an empty or a repeated Short Name from now on.
            SQLException duplicate = assertThrows(SQLException.class, () -> insertOffer(connection,
                    "Doppelt", "Ort", "horyzon-kolumbien-palastina"));
            assertThat(duplicate.getMessage(), containsString("UNIQUE"));
            SQLException missing = assertThrows(SQLException.class, () -> insertOffer(connection,
                    "Ohne", "Ort", null));
            assertThat(missing.getMessage(), containsString("NOT NULL"));
        }
    }

    private static org.flywaydb.core.api.configuration.FluentConfiguration flyway(String url) {
        return Flyway.configure().dataSource(url, null, null).locations("classpath:db/migration");
    }

    private static void insertOffer(Connection connection, String organization, String location) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO voluntary_services (organization, organizationLink, location, description) VALUES (?, 'https://example.org', ?, 'Beschreibung')")) {
            insert.setString(1, organization);
            insert.setString(2, location);
            insert.executeUpdate();
        }
    }

    private static void insertOffer(Connection connection, String organization, String location, String slug) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO voluntary_services (organization, organizationLink, location, description, slug) VALUES (?, 'https://example.org', ?, 'Beschreibung', ?)")) {
            insert.setString(1, organization);
            insert.setString(2, location);
            insert.setString(3, slug);
            insert.executeUpdate();
        }
    }

    private static Map<Integer, String> slugs(Connection connection) throws SQLException {
        Map<Integer, String> slugs = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT id, slug FROM voluntary_services ORDER BY id")) {
            while (rows.next()) {
                slugs.put(rows.getInt("id"), rows.getString("slug"));
            }
        }
        return slugs;
    }
}
