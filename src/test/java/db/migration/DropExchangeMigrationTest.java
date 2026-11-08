package db.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * FR-029 rejected (OQ-005): the migration removes the {@code exchanges} table, also when it still
 * holds rows, and leaves the tables of the entity model in place.
 */
class DropExchangeMigrationTest {

    @TempDir
    Path directory;

    @Test
    void the_exchange_table_is_dropped_and_the_other_tables_stay() throws SQLException {
        String url = "jdbc:sqlite:" + directory.resolve("migration.db");
        flyway(url).target("1.0.7").load().migrate();

        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO exchanges (organization, organizationLink, description)
                    VALUES ('Partner', 'https://example.org', 'Beschreibung')""");
            assertThat(tableExists(statement, "exchanges"), is(true));

            flyway(url).load().migrate();

            assertThat(tableExists(statement, "exchanges"), is(false));
            for (String table : new String[]{"events", "voluntary_services", "contact_form_entries", "users"}) {
                assertThat(table, tableExists(statement, table), is(true));
            }
        }
    }

    private static boolean tableExists(Statement statement, String table) throws SQLException {
        try (ResultSet row = statement.executeQuery(
                "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = '" + table + "'")) {
            row.next();
            return row.getInt(1) == 1;
        }
    }

    private static org.flywaydb.core.api.configuration.FluentConfiguration flyway(String url) {
        return Flyway.configure().dataSource(url, null, null).locations("classpath:db/migration");
    }
}
