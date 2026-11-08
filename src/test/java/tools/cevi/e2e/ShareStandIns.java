package tools.cevi.e2e;

/**
 * Init scripts that stand in for the browser's sharing facilities in the share tests of UC-009
 * and UC-011.
 *
 * <p>Headless Chromium offers no Share Dialog of its own, so the tests that need one install a
 * stand-in for {@code navigator.share} before the page loads; it records what it was given in
 * {@code window.__shared} and answers like the real dialog would: resolved when an app was
 * chosen, an {@code AbortError} when the visitor cancelled.
 */
final class ShareStandIns {

    /** The browser offers no Share Dialog at all. */
    static final String NO_SHARE_DIALOG = "delete Navigator.prototype.share; delete navigator.share;";

    /** No Share Dialog, and a clipboard that refuses the address. */
    static final String REFUSING_CLIPBOARD = NO_SHARE_DIALOG
            + "Object.defineProperty(navigator, 'clipboard', { value: { writeText: function () {"
            + " return Promise.reject(new DOMException('refused', 'NotAllowedError')); } } });";

    private ShareStandIns() {
    }

    /** A Share Dialog that records its arguments and ends the way {@code outcome} says. */
    static String shareDialog(Outcome outcome) {
        return "window.__shared = [];"
                + "navigator.share = function (data) { window.__shared.push(data); "
                + switch (outcome) {
                    case SENT -> "return Promise.resolve(); };";
                    case CANCELLED -> "return Promise.reject(new DOMException('cancelled', 'AbortError')); };";
                    case FAILED -> "return Promise.reject(new DOMException('failed', 'NotAllowedError')); };";
                };
    }

    enum Outcome { SENT, CANCELLED, FAILED }
}
