package tools.cevi.infra;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccentVariantTest {

    @Test
    void the_same_key_always_gets_the_same_variant() {
        assertEquals(AccentVariant.of("euro-camp-2026"), AccentVariant.of("euro-camp-2026"));
    }

    @Test
    void every_variant_is_within_range() {
        for (String key : new String[]{"a", "euro-camp-2026", "ymca-europe-festival", "", "ä"}) {
            int variant = AccentVariant.of(key);
            assertTrue(variant >= 0 && variant < AccentVariant.COUNT,
                    "variant for '" + key + "' was " + variant);
        }
    }

    @Test
    void a_missing_key_falls_back_rather_than_failing() {
        assertEquals(0, AccentVariant.of(null));
        assertEquals(0, AccentVariant.of(""));
    }

    /**
     * The point of the variant is that a list does not read as identical slabs, so all four have to
     * actually occur across a realistic set of slugs.
     */
    @Test
    void a_list_of_slugs_uses_all_four_variants() {
        Set<Integer> seen = new HashSet<>();
        for (int i = 1; i <= 40; i++) {
            seen.add(AccentVariant.of("anlass-" + i));
        }

        assertEquals(AccentVariant.COUNT, seen.size(), "seen: " + seen);
    }

    /** A negative hash must not produce a negative variant — hence floorMod, not %. */
    @Test
    void keys_hashing_negative_still_yield_a_usable_variant() {
        String negative = "polygenelubricants";
        assertTrue(negative.hashCode() < 0, "precondition: this key hashes negative");

        assertTrue(AccentVariant.of(negative) >= 0);
    }
}
