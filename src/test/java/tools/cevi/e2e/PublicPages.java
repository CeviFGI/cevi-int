package tools.cevi.e2e;

import java.util.List;

/**
 * The routes a visitor reaches without signing in.
 *
 * <p>Named once rather than repeated as a {@code @ValueSource} per test: every quality gate phase 5
 * adds runs over the same list, so a page added later is picked up by all of them at once instead
 * of by whichever tests someone remembered to extend.
 *
 * <p>The event detail page is deliberately not here — it needs a record, so the tests that want it
 * create one of their own with the content they are testing for.
 */
final class PublicPages {

    static final List<String> ROUTES = List.of(
            "/",
            "/anlaesse",
            "/volontariat",
            "/kontakt",
            "/fgi",
            "/datenschutzinformation",
            "/auth/login",
            "/version");

    private PublicPages() {
    }
}
