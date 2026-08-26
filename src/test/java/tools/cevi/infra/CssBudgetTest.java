package tools.cevi.infra;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps the redesign from turning into a page-weight regression (NFR-041).
 *
 * <p>The budget is deliberately measured over what the server actually hands out, following the
 * {@code @import} list of {@code site.css} rather than a hard-coded set of file names — a
 * stylesheet that is added but never imported would otherwise be invisible here, and one that is
 * imported but missing would go unnoticed until a page rendered wrong.
 */
@QuarkusTest
class CssBudgetTest {

    private static final int BUDGET_BYTES_GZIPPED = 24 * 1024;
    private static final Pattern IMPORT = Pattern.compile("@import\\s+\"([^\"]+)\"");
    private static final Pattern HEX_COLOUR = Pattern.compile("(#[0-9A-Fa-f]{3,8})\\b");
    private static final Pattern FUNCTIONAL_COLOUR = Pattern.compile("\\b(rgba?\\(|hsla?\\(|oklch\\()");
    private static final Pattern FONT_SIZE = Pattern.compile("font-size:\\s*([^;]+);");

    @Test
    void the_stylesheet_bundle_stays_within_its_budget() {
        String hub = fetch("/css/site.css");

        int sum = gzippedSize(hub);
        for (String imported : importsOf(hub)) {
            sum += gzippedSize(fetch("/css/" + imported));
        }
        final int total = sum;

        assertTrue(total <= BUDGET_BYTES_GZIPPED,
                () -> "all stylesheets a public page loads come to " + total
                        + " bytes gzipped, over the " + BUDGET_BYTES_GZIPPED + " byte budget (NFR-041)");
    }

    /**
     * The token file is the single source of colour and type (NFR-037). This does not prove that
     * no component file carries a literal value — that is a review question — but it does catch
     * the common regression of a second file starting to declare {@code :root} variables.
     */
    @Test
    void only_the_token_file_declares_custom_properties_on_the_root() {
        String hub = fetch("/css/site.css");

        for (String imported : importsOf(hub)) {
            String css = fetch("/css/" + imported);
            if (imported.equals("tokens.css")) {
                assertTrue(css.contains("--cevi-red:"),
                        "tokens.css should declare the brand colours");
            } else {
                assertFalse(css.contains(":root"),
                        imported + " must read tokens, not declare them (NFR-037)");
            }
        }
    }

    /**
     * The other half of NFR-037, and the half that had quietly stopped being true: the token file
     * is the single source of colour and type <em>size</em>, so no other stylesheet may state one
     * itself. Six literal colours had accumulated across five files by the end of phase 4 — none
     * of them wrong to look at, all of them a value that could no longer be changed in one place.
     *
     * <p>Two forms are allowed through and both are named rather than filtered silently: a colour
     * inside a {@code data:} URI, which is an inline SVG the browser paints and not a rule anyone
     * would go looking for in the palette, and a font size in {@code em}, which is a multiplier of
     * the inherited step rather than a size of its own.
     */
    @Test
    void no_stylesheet_but_the_token_file_states_a_colour_or_a_size_of_its_own() {
        String hub = fetch("/css/site.css");

        List<String> stated = new ArrayList<>();
        for (String imported : importsOf(hub)) {
            if (imported.equals("tokens.css")) {
                continue;
            }
            String css = withoutCommentsAndInlineImages(fetch("/css/" + imported));

            for (String literal : matches(HEX_COLOUR, css)) {
                stated.add(imported + " states the colour " + literal);
            }
            for (String literal : matches(FUNCTIONAL_COLOUR, css)) {
                stated.add(imported + " states the colour " + literal);
            }
            for (String size : matches(FONT_SIZE, css)) {
                if (!size.contains("var(--") && !size.trim().endsWith("em")) {
                    stated.add(imported + " states the font size " + size.trim());
                }
            }
        }

        assertTrue(stated.isEmpty(),
                () -> "every colour and type size belongs in tokens.css (NFR-037):\n  "
                        + String.join("\n  ", stated));
    }

    private static List<String> matches(Pattern pattern, String css) {
        return pattern.matcher(css).results().map(result -> result.group(1)).toList();
    }

    /**
     * A {@code data:} URI holds an SVG the stylesheet draws inline — the triangle motif and the
     * location pin. Its fill is part of the drawing, not part of the palette.
     */
    private static String withoutCommentsAndInlineImages(String css) {
        return css.replaceAll("(?s)/\\*.*?\\*/", "")
                  .replaceAll("url\\(\"data:[^)]*\\)", "");
    }

    private static List<String> importsOf(String css) {
        Matcher matcher = IMPORT.matcher(css);
        return matcher.results().map(result -> result.group(1)).toList();
    }

    private static String fetch(String path) {
        return given().when().get(path).then().statusCode(200).extract().body().asString();
    }

    private static int gzippedSize(String content) {
        try (ByteArrayOutputStream sink = new ByteArrayOutputStream()) {
            try (GZIPOutputStream gzip = new GZIPOutputStream(sink)) {
                gzip.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            return sink.size();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
