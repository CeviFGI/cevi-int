package tools.cevi.infra;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * The moment the system records as creation and Last Change of an entry (UC-013 BR-004), cut to
 * the milliseconds the database keeps, so that an entry reads the same before and after it is stored.
 */
public final class Changes {

    private Changes() {
    }

    public static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
