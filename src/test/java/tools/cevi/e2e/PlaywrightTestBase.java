package tools.cevi.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Base class for real-browser e2e tests. Runs the actual Quarkus HTTP server
 * (like the existing {@code @QuarkusTest} REST Assured tests) and drives it with a
 * real headless Chromium via Playwright. Every test gets a fresh, isolated
 * {@link BrowserContext}. A trace is recorded for every test but only kept on disk
 * (under {@code target/playwright-traces/}) when the test fails, ready to be
 * inspected with the Playwright Trace Viewer or uploaded as a CI artifact.
 */
@QuarkusTest
@ExtendWith(PlaywrightTestBase.PlaywrightLifecycle.class)
public abstract class PlaywrightTestBase {

    private static Playwright playwright;
    private static Browser browser;

    @TestHTTPResource
    URL baseUrl;

    protected BrowserContext context;
    protected Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        boolean headed = Boolean.getBoolean("playwright.headed");
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(!headed));
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    /** Resolves a path against the running test server, e.g. {@code url("/anlaesse")}. */
    protected String url(String path) {
        String base = baseUrl.toString();
        if (base.endsWith("/") && path.startsWith("/")) {
            return base + path.substring(1);
        }
        return base + path;
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    /**
     * Implemented as an Extension (rather than plain {@code @BeforeEach}/{@code @AfterEach}
     * methods) so the callbacks receive {@link ExtensionContext} directly per the Extension
     * SPI contract instead of relying on JUnit5's parameter resolution, which does not
     * support injecting {@code ExtensionContext} into lifecycle methods.
     */
    static class PlaywrightLifecycle implements BeforeEachCallback, AfterEachCallback, TestWatcher {
        private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(PlaywrightTestBase.class);
        private static final Path TRACE_DIR = Paths.get("target", "playwright-traces");

        @Override
        public void beforeEach(ExtensionContext extensionContext) {
            PlaywrightTestBase instance = (PlaywrightTestBase) extensionContext.getRequiredTestInstance();
            instance.context = browser.newContext();
            instance.context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
            instance.page = instance.context.newPage();
        }

        @Override
        public void afterEach(ExtensionContext extensionContext) throws IOException {
            PlaywrightTestBase instance = (PlaywrightTestBase) extensionContext.getRequiredTestInstance();
            Files.createDirectories(TRACE_DIR);
            Path tracePath = TRACE_DIR.resolve(sanitize(extensionContext.getUniqueId()) + ".zip");
            try {
                instance.context.tracing().stop(new Tracing.StopOptions().setPath(tracePath));
            } finally {
                instance.context.close();
            }
            extensionContext.getStore(NAMESPACE).put("tracePath", tracePath);
        }

        @Override
        public void testSuccessful(ExtensionContext context) {
            Object tracePath = context.getStore(NAMESPACE).get("tracePath");
            if (tracePath instanceof Path path) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // best-effort cleanup only
                }
            }
        }

        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            Object tracePath = context.getStore(NAMESPACE).get("tracePath");
            if (tracePath != null) {
                System.err.println("Playwright trace saved to: " + tracePath);
            }
        }
    }
}
