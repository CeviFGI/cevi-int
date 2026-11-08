package tools.cevi.feed;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/** UC-013 BR-003: the parts of a Feed Item that do not need a running application. */
class FeedItemTest {

    @Test
    void plain_text_is_escaped_as_html() {
        assertThat(FeedItem.escape("<a href=\"x\">Tom's & Jerry's</a>"),
                equalTo("&lt;a href=&quot;x&quot;&gt;Tom&#39;s &amp; Jerry&#39;s&lt;/a&gt;"));
    }

    @Test
    void the_moment_of_publication_follows_rfc_822_in_utc() {
        FeedItem item = new FeedItem("t", "l", "g", "Anlass", Instant.parse("2026-11-08T09:05:03.250Z"), "");

        assertThat(item.pubDate(), equalTo("Sun, 8 Nov 2026 09:05:03 GMT"));
    }
}
