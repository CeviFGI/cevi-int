package tools.cevi.infra;

/**
 * Picks one of four accent treatments for a card, from a key the record already has.
 *
 * <p>Neither an event nor an offer carries an image, and adding one is deliberately out of scope
 * (see {@code docs/ux_concept.md} §8). The accent band is therefore what keeps a list of five
 * entries from reading as five identical slabs. Deriving it from the slug rather than from the
 * position in the list means the same event always shows the same colour: it becomes a weak
 * recognition cue for a returning visitor, and it does not change when a new event is published
 * above it.
 */
public final class AccentVariant {

    public static final int COUNT = 4;

    private AccentVariant() {
    }

    /**
     * @param key a stable identifier of the record — the slug for an event, the id for an offer
     * @return a variant in {@code [0, COUNT)}, the same one for the same key on every JVM
     *         ({@link String#hashCode()} is specified, not implementation-defined)
     */
    public static int of(String key) {
        if (key == null || key.isEmpty()) {
            return 0;
        }
        return Math.floorMod(key.hashCode(), COUNT);
    }
}
