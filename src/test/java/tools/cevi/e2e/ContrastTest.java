package tools.cevi.e2e;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The arithmetic behind {@link ContrastE2ETest}, checked against the figures WCAG itself publishes.
 * A contrast test that computes the ratio wrongly is worse than no contrast test: it reports a
 * clean sheet on a page nobody can read.
 *
 * <p>Not an {@code *E2ETest}, so Surefire runs it — there is no browser involved.
 */
class ContrastTest {

    private static final double PRECISION = 0.02;

    @Test
    void black_on_white_is_the_maximum_ratio() {
        assertEquals(21.0, Contrast.parse("rgb(0, 0, 0)")
                .ratioAgainst(Contrast.parse("rgb(255, 255, 255)")), PRECISION);
    }

    @Test
    void a_colour_against_itself_is_one_to_one() {
        Contrast red = Contrast.parse("rgb(196, 19, 51)");
        assertEquals(1.0, red.ratioAgainst(red), PRECISION);
    }

    @Test
    void the_ratio_does_not_depend_on_which_colour_is_named_first() {
        Contrast blue = Contrast.parse("rgb(50, 51, 148)");
        Contrast white = Contrast.parse("rgb(255, 255, 255)");
        assertEquals(blue.ratioAgainst(white), white.ratioAgainst(blue), PRECISION);
    }

    /** The brand colours, with the figures {@code tokens.css} states in its own header comment. */
    @Test
    void the_brand_colours_measure_what_the_token_file_claims() {
        Contrast white = Contrast.parse("rgb(255, 255, 255)");

        assertEquals(18.4, Contrast.parse("rgb(20, 20, 18)").ratioAgainst(white), 0.1);
        assertEquals(6.93, Contrast.parse("rgb(90, 90, 86)").ratioAgainst(white), 0.1);
        assertEquals(10.31, Contrast.parse("rgb(50, 51, 148)").ratioAgainst(white), 0.1);
        assertEquals(6.02, Contrast.parse("rgb(196, 19, 51)").ratioAgainst(white), 0.1);
    }

    @Test
    void rgba_is_composited_over_what_lies_behind_it() {
        Contrast halfBlack = Contrast.parse("rgba(0, 0, 0, 0.5)");
        Contrast onWhite = halfBlack.over(Contrast.parse("rgb(255, 255, 255)"));

        assertEquals(127.5, onWhite.red(), 0.01);
        assertEquals(1.0, onWhite.alpha(), 0.01);
    }

    @Test
    void the_opacity_property_fades_a_colour_towards_its_backdrop() {
        Contrast white = Contrast.parse("rgb(255, 255, 255)");
        Contrast red = Contrast.parse("rgb(196, 19, 51)");

        Contrast faded = white.fadedTo(0.5, red);

        assertEquals((255 + 19) / 2.0, faded.green(), 0.01);
        assertTrue(faded.ratioAgainst(red) < white.ratioAgainst(red),
                "fading text towards its own background can only reduce the contrast");
    }

    @Test
    void a_fully_transparent_colour_paints_nothing() {
        assertTrue(Contrast.parse("rgba(0, 0, 0, 0)").transparent());
        assertFalse(Contrast.parse("rgb(0, 0, 0)").transparent());
    }

    @Test
    void the_space_separated_form_modern_browsers_report_is_understood() {
        assertEquals(Contrast.parse("rgba(196, 19, 51, 0.5)"), Contrast.parse("rgb(196 19 51 / 0.5)"));
    }

    /** SC 1.4.3 relaxes to 3 : 1 for large text — 24 px, or 18.66 px when it is bold. */
    @Test
    void large_text_may_sit_at_three_to_one() {
        assertEquals(4.5, Contrast.thresholdFor(16, 400), PRECISION);
        assertEquals(4.5, Contrast.thresholdFor(23.9, 400), PRECISION);
        assertEquals(3.0, Contrast.thresholdFor(24, 400), PRECISION);
        assertEquals(4.5, Contrast.thresholdFor(18.66, 400), PRECISION);
        assertEquals(3.0, Contrast.thresholdFor(18.66, 700), PRECISION);
    }
}
