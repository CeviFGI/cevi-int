package tools.cevi.infra;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
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

    private static final int BUDGET_BYTES_GZIPPED = 12 * 1024;
    private static final Pattern IMPORT = Pattern.compile("@import\\s+\"([^\"]+)\"");

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
