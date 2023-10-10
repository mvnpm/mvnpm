package io.mvnpm;

import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;

import io.quarkiverse.playwright.InjectPlaywright;
import io.quarkiverse.playwright.WithPlaywright;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithPlaywright
public class UITest {

    @InjectPlaywright
    BrowserContext context;

    @TestHTTPResource("/")
    URL index;

    @TestHTTPResource("/about")
    URL about;

    @Test
    public void testIndex() {
        final Page page = context.newPage();
        Response response = page.navigate(index.toString());
        Assertions.assertEquals("OK", response.statusText());

        page.waitForLoadState();

        String title = page.title();
        Assertions.assertEquals("mvnpm", title);

        final ElementHandle coordinatesInputEl = page.waitForSelector("#coordinates-field input");
        coordinatesInputEl.click();
        coordinatesInputEl.fill("lit");
        coordinatesInputEl.press("Enter");
        final ElementHandle depEl = page.waitForSelector("#pom-dependency-code");
        Assertions.assertTrue(depEl.innerText().contains("<artifactId>lit</artifactId>"),
                "contains <artifactId>lit</artifactId>");

    }

    @Test
    public void testAbout() {
        final Page page = context.newPage();
        Response response = page.navigate(about.toString());
        Assertions.assertEquals("OK", response.statusText());

        page.waitForLoadState();

        String title = page.title();
        Assertions.assertEquals("mvnpm", title);
        page.getByText("mvnpm", new Page.GetByTextOptions().setExact(true)).elementHandle();
    }

}
