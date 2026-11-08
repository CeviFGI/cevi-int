package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import tools.cevi.infra.Slug;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Gives every voluntary service offer a Short Name (FR-044). An offer
 * recorded before this revision receives one derived from its Organisation and location, so that no
 * row is left without a Public Address (entity model, VOLUNTARY_SERVICE constraints).
 * <p>
 * A Java migration rather than SQL: SQLite cannot strip diacritics, and the derived name has to
 * read exactly like one the application derives.
 */
@SuppressWarnings("java:S101") // Flyway takes the version from the class name.
public class V1_0_5__VoluntaryServiceSlug extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws SQLException {
        Connection connection = context.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE voluntary_services ADD COLUMN slug TEXT");
        }

        Map<Integer, String> slugs = deriveSlugs(connection);
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE voluntary_services SET slug = ? WHERE id = ?")) {
            for (Map.Entry<Integer, String> entry : slugs.entrySet()) {
                update.setString(1, entry.getValue());
                update.setInt(2, entry.getKey());
                update.addBatch();
            }
            update.executeBatch();
        }

        // SQLite cannot add NOT NULL and UNIQUE to an existing column, so the table is rebuilt.
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE voluntary_services_new (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      description TEXT NOT NULL,
                      location TEXT NOT NULL,
                      organization TEXT NOT NULL,
                      organizationLink TEXT NOT NULL,
                      slug TEXT NOT NULL UNIQUE
                    )""");
            statement.execute("""
                    INSERT INTO voluntary_services_new (id, description, location, organization, organizationLink, slug)
                    SELECT id, description, location, organization, organizationLink, slug FROM voluntary_services""");
            statement.execute("DROP TABLE voluntary_services");
            statement.execute("ALTER TABLE voluntary_services_new RENAME TO voluntary_services");
        }
    }

    // UC-004 BR-007: Organisation followed by location, a counter appended until the name is free;
    // the oldest offer keeps the plain name.
    private static Map<Integer, String> deriveSlugs(Connection connection) throws SQLException {
        Map<Integer, String> slugs = new LinkedHashMap<>();
        Set<String> taken = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT id, organization, location FROM voluntary_services ORDER BY id")) {
            while (rows.next()) {
                String slug = Slug.unique(rows.getString("organization") + " " + rows.getString("location"),
                        taken::contains);
                if (slug.isEmpty()) {
                    // Nothing readable to derive from (e.g. only non-Latin script): the id is
                    // unique and keeps the column free of empty values, as V1.0.4 did for events.
                    slug = Slug.unique(rows.getString("id"), taken::contains);
                }
                taken.add(slug);
                slugs.put(rows.getInt("id"), slug);
            }
        }
        return slugs;
    }
}
