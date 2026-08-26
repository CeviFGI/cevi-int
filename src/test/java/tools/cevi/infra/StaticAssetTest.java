package tools.cevi.infra;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every asset a stylesheet or a page names has to exist. A missing font degrades silently to a
 * fallback face and a missing image to a broken box, so neither shows up as a failing request
 * anywhere else.
 */
@QuarkusTest
class StaticAssetTest {

    /** The two faces a German page always needs — the budget in NFR-041 is stated against these. */
    private static final List<String> BASELINE_FONTS =
            List.of("/fonts/montserrat-latin.woff2", "/fonts/lora-latin.woff2");

    private static final int BASELINE_FONT_BUDGET_BYTES = 90 * 1024;

    /** Comfortably above the 5.8 KB the current asset costs, well below a full-size original. */
    private static final int LOGO_BUDGET_BYTES = 12 * 1024;

    private static final Pattern CSS_URL = Pattern.compile("url\\((/[^)\"']+)\\)");
    private static final Pattern IMPORT = Pattern.compile("@import\\s+\"([^\"]+)\"");

    @Test
    void every_asset_referenced_from_a_stylesheet_is_served() {
        Set<String> assets = new LinkedHashSet<>();
        String hub = fetch("/css/site.css");
        collectUrls(hub, assets);
        for (String imported : imports(hub)) {
            collectUrls(fetch("/css/" + imported), assets);
        }

        assertFalse(assets.isEmpty(), "the stylesheets should reference at least the web fonts");
        for (String asset : assets) {
            given().when().get(asset).then()
                    .statusCode(200);
        }
    }

    /**
     * The header renders the logo at 28 px on a phone and 40 px from 600 px up, so the asset is
     * sized for the taller of the two on a high-density screen — 674 × 80 — and nothing more.
     * It shipped as the 1500 × 178 original for years: 29 KB of which every visitor downloaded
     * all and saw about a twentieth, and a downscale the browser had to do on every page.
     */
    @Test
    void the_logo_referenced_by_every_page_is_served_at_the_size_it_is_shown() {
        byte[] logo = given().when().get("/logo.png").then()
                .statusCode(200)
                .extract().body().asByteArray();

        assertTrue(logo.length <= LOGO_BUDGET_BYTES,
                () -> "the logo is " + logo.length + " bytes, over the " + LOGO_BUDGET_BYTES
                        + " byte budget — it is drawn at most 40 CSS px tall (NFR-041)");
        assertEquals(674, pngWidth(logo),
                "a logo wider than twice its rendered width is bytes the visitor cannot see");
    }

    /**
     * The extended and italic faces are declared but fetched only when rendered text needs them,
     * so the figure that has to stay inside the budget is the baseline pair, not the whole
     * directory (NFR-041).
     */
    @Test
    void the_fonts_a_german_page_always_loads_stay_within_the_budget() {
        int total = 0;
        for (String font : BASELINE_FONTS) {
            byte[] body = given().when().get(font).then()
                    .statusCode(200)
                    .extract().body().asByteArray();
            total += body.length;
        }

        final int measured = total;
        assertTrue(measured <= BASELINE_FONT_BUDGET_BYTES,
                () -> "montserrat-latin plus lora-latin come to " + measured
                        + " bytes, over the " + BASELINE_FONT_BUDGET_BYTES + " byte budget (NFR-041)");
    }

    /**
     * The licence has to travel with the font: SIL OFL-1.1 requires it, and C-018 admits the fonts
     * on that basis.
     */
    @Test
    void the_font_licences_are_present() {
        given().when().get("/fonts/OFL-Montserrat.txt").then().statusCode(200);
        given().when().get("/fonts/OFL-Lora.txt").then().statusCode(200);
    }

    /** Bytes 16..23 of a PNG are the IHDR width and height, big-endian. */
    private static int pngWidth(byte[] png) {
        return ByteBuffer.wrap(png, 16, 4).getInt();
    }

    private static void collectUrls(String css, Set<String> into) {
        Matcher matcher = CSS_URL.matcher(css);
        while (matcher.find()) {
            into.add(matcher.group(1));
        }
    }

    private static List<String> imports(String css) {
        return IMPORT.matcher(css).results().map(result -> result.group(1)).toList();
    }

    private static String fetch(String path) {
        return given().when().get(path).then().statusCode(200).extract().body().asString();
    }
}
