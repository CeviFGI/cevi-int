package tools.cevi.e2e;

import java.util.Locale;

/**
 * WCAG 2.1 relative luminance and contrast ratio (SC 1.4.3).
 *
 * <p>Deliberately Java rather than a few lines inside the page script: the browser can report what
 * a colour resolved to, but the arithmetic that turns two colours into a ratio is the part that
 * can be wrong in a way no page would reveal — so it lives where {@link ContrastTest} can check it
 * against the published worked examples.
 */
record Contrast(double red, double green, double blue, double alpha) {

    /** Parses the {@code rgb(r, g, b)} / {@code rgba(r, g, b, a)} form every browser computes to. */
    static Contrast parse(String computed) {
        String digits = computed.trim()
                .replace("rgba(", "").replace("rgb(", "").replace(")", "")
                .replace("/", " ").replace(",", " ");
        String[] parts = digits.trim().split("\\s+");
        double alpha = parts.length > 3 ? Double.parseDouble(parts[3]) : 1.0;
        return new Contrast(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]), alpha);
    }

    boolean transparent() {
        return alpha == 0.0;
    }

    /** This colour laid over {@code backdrop} at its own alpha — the source-over composite. */
    Contrast over(Contrast backdrop) {
        if (alpha >= 1.0) {
            return this;
        }
        return new Contrast(
                red * alpha + backdrop.red * (1 - alpha),
                green * alpha + backdrop.green * (1 - alpha),
                blue * alpha + backdrop.blue * (1 - alpha),
                1.0);
    }

    /** The same, for the {@code opacity} property, which composites the element as a whole. */
    Contrast fadedTo(double opacity, Contrast backdrop) {
        return new Contrast(red, green, blue, alpha * opacity).over(backdrop);
    }

    double luminance() {
        return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue);
    }

    private static double channel(double value) {
        double srgb = value / 255.0;
        return srgb <= 0.04045 ? srgb / 12.92 : Math.pow((srgb + 0.055) / 1.055, 2.4);
    }

    double ratioAgainst(Contrast other) {
        double a = luminance();
        double b = other.luminance();
        double lighter = Math.max(a, b);
        double darker = Math.min(a, b);
        return (lighter + 0.05) / (darker + 0.05);
    }

    /**
     * WCAG SC 1.4.3: 18 pt, or 14 pt bold, may sit at 3 : 1 instead of 4.5 : 1. In CSS pixels that
     * is 24 px, or 18.66 px at weight 700 and above.
     */
    static double thresholdFor(double fontSizePx, int fontWeight) {
        boolean large = fontSizePx >= 24.0 || (fontSizePx >= 18.66 && fontWeight >= 700);
        return large ? 3.0 : 4.5;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "rgb(%.0f %.0f %.0f)", red, green, blue);
    }
}
