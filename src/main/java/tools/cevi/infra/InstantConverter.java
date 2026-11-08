package tools.cevi.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Stores a moment as UTC text of fixed width, {@code 2026-11-08T09:15:00.000Z}. SQLite has no type
 * of its own for it, and a fixed width — milliseconds always written — keeps the text in the order
 * of the moments, so the database can sort by it (UC-013 BR-002). The migration writes the same
 * form ({@code V1.0.7__LastChange.sql}).
 */
@Converter(autoApply = true)
public class InstantConverter implements AttributeConverter<Instant, String> {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    @Override
    public String convertToDatabaseColumn(Instant instant) {
        return instant != null ? FORMATTER.format(instant) : null;
    }

    @Override
    public Instant convertToEntityAttribute(String dbData) {
        return dbData != null && !dbData.isEmpty() ? Instant.from(FORMATTER.parse(dbData)) : null;
    }
}
