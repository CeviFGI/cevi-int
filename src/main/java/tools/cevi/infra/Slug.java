package tools.cevi.infra;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Slug {
    /** The stored limit of a Short Name (UC-002 BR-003, UC-004 BR-004). */
    public static final int MAX_LENGTH = 255;
    /** A derived name is cut here, so that name and counter together stay within {@link #MAX_LENGTH}. */
    public static final int MAX_DERIVED_LENGTH = 252;

    private final String value;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s\\p{Punct}&&[^-]]");

    private Slug(String text) {
        String noseparators = SEPARATORS.matcher(text).replaceAll("-");
        String normalized = Normalizer.normalize(noseparators, Normalizer.Form.NFD);
        String latin = NONLATIN.matcher(normalized).replaceAll("");
        this.value = latin.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}","-").replaceAll("(^-)|(-$)","");
    }

    public static Slug of(String text) {
        return new Slug(text);
    }

    /**
     * Derives a readable Short Name from {@code text}, cut to {@link #MAX_DERIVED_LENGTH} characters,
     * and appends a counter until {@code taken} no longer claims it. Events (UC-002 BR-005, BR-006)
     * and offers (UC-004 BR-007) derive their Short Names this way.
     */
    public static String unique(String text, Predicate<String> taken) {
        String base = of(text == null ? "" : text).toString();
        base = base.substring(0, Math.min(base.length(), MAX_DERIVED_LENGTH));
        String slug = base;
        int counter = 0;
        while (taken.test(slug)) {
            String suffix = Integer.toString(counter++);
            slug = base.substring(0, Math.min(base.length(), MAX_LENGTH - suffix.length())) + suffix;
        }
        return slug;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
