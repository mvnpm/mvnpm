package io.mvnpm;

import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitForSelectorState;

import io.quarkiverse.playwright.InjectPlaywright;
import io.quarkiverse.playwright.WithPlaywright;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithPlaywright
public class UITest {

    @InjectPlaywright
    BrowserContext context;

    @TestHTTPResource("/")
    URL index;

    @TestHTTPResource("/doc")
    URL doc;

    @TestHTTPResource("/about")
    URL about;

    @TestHTTPResource("/releases")
    URL releases;

    @TestHTTPResource("/live")
    URL live;

    @TestHTTPResource("/composites")
    URL composites;

    @Test
    @DisabledOnIntegrationTest
    public void testIndex() {
        final Page page = context.newPage();
        Response response = page.navigate(index.toString());
        Assertions.assertEquals("OK", response.statusText());

        page.waitForLoadState();

        String title = page.title();
        Assertions.assertEquals(
                "mvnpm - Use NPM packages as Maven/Gradle dependencies",
                title);

        // Check that the SPA component is present in the DOM
        page.waitForSelector("mvnpm-home",
                new Page.WaitForSelectorOptions().setState(WaitForSelectorState.ATTACHED));

    }

    @Test
    public void testDoc() {
        final Page page = context.newPage();
        Response response = page.navigate(doc.toString());
        Assertions.assertEquals("OK", response.statusText());

        page.waitForLoadState();

        String title = page.title();
        Assertions.assertTrue(title.contains("Getting Started with mvnpm"),
                "Doc page title should contain 'Getting Started with mvnpm'");
        page.getByText("Add an NPM dependency", new Page.GetByTextOptions().setExact(true))
                .elementHandle();
    }

    @Test
    public void testAbout() {
        final Page page = context.newPage();
        Response response = page.navigate(about.toString());
        Assertions.assertEquals("OK", response.statusText());

        page.waitForLoadState();

        String title = page.title();
        Assertions.assertTrue(title.contains("About mvnpm"),
                "About page title should contain 'About mvnpm'");
        page.getByText("The story", new Page.GetByTextOptions().setExact(true))
                .elementHandle();
        page.getByText("Why not WebJars?", new Page.GetByTextOptions().setExact(true))
                .elementHandle();
        page.getByText("The ecosystem", new Page.GetByTextOptions().setExact(true))
                .elementHandle();
        page.getByText("The authors", new Page.GetByTextOptions().setExact(true))
                .elementHandle();
    }

    @Test
    public void testReleases() {
        final Page page = context.newPage();
        Response response = page.navigate(releases.toString());
        Assertions.assertEquals("OK", response.statusText());

        page.waitForLoadState();

        String title = page.title();
        Assertions.assertTrue(title.contains("Releases"),
                "Releases page title should contain 'Releases'");
        // Check that the SPA component rendered
        page.waitForSelector("mvnpm-releases",
                new Page.WaitForSelectorOptions().setState(WaitForSelectorState.ATTACHED));
    }

    @Test
    public void testLive() {
        final Page page = context.newPage();
        Response response = page.navigate(live.toString());
        Assertions.assertEquals("OK", response.statusText());

        page.waitForLoadState();

        String title = page.title();
        Assertions.assertTrue(title.contains("Live"),
                "Live page title should contain 'Live'");
        page.waitForSelector("mvnpm-live",
                new Page.WaitForSelectorOptions().setState(WaitForSelectorState.ATTACHED));
    }

    @Test
    public void testComposites() {
        final Page page = context.newPage();
        Response response = page.navigate(composites.toString());
        Assertions.assertEquals("OK", response.statusText());

        page.waitForLoadState();

        String title = page.title();
        Assertions.assertTrue(title.contains("Composites"),
                "Composites page title should contain 'Composites'");
        page.waitForSelector("mvnpm-composites",
                new Page.WaitForSelectorOptions().setState(WaitForSelectorState.ATTACHED));
    }

    @Test
    public void testAboutMobileLayout() {
        final Page page = context.newPage();
        page.setViewportSize(375, 812);
        Response response = page.navigate(about.toString());
        Assertions.assertEquals("OK", response.statusText());

        page.waitForLoadState();

        // On mobile, body should allow normal scrolling (not overflow: hidden)
        String bodyOverflow = (String) page.evaluate(
                "() => window.getComputedStyle(document.body).overflowY");
        Assertions.assertNotEquals("hidden", bodyOverflow,
                "Body should not have overflow-y: hidden on mobile");

        // Main should also allow normal scrolling
        String mainOverflow = (String) page.evaluate(
                "() => window.getComputedStyle(document.querySelector('main')).overflow");
        Assertions.assertNotEquals("hidden", mainOverflow,
                "Main should not have overflow: hidden on mobile");

        // Nav should still be visible
        page.waitForSelector("nav");

        // Content sections should be visible
        page.getByText("The story", new Page.GetByTextOptions().setExact(true))
                .elementHandle();
    }

    @Test
    public void testDocMobileLayout() {
        final Page page = context.newPage();
        page.setViewportSize(375, 812);
        Response response = page.navigate(doc.toString());
        Assertions.assertEquals("OK", response.statusText());

        page.waitForLoadState();

        String bodyOverflow = (String) page.evaluate(
                "() => window.getComputedStyle(document.body).overflowY");
        Assertions.assertNotEquals("hidden", bodyOverflow,
                "Body should not have overflow-y: hidden on mobile");

        page.getByText("Add an NPM dependency", new Page.GetByTextOptions().setExact(true))
                .elementHandle();
    }

}
