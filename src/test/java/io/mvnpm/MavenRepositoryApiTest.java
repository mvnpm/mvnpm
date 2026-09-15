package io.mvnpm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MavenRepositoryApiTest {

    // Plain NIO rather than an injected Vertx: MavenRepositoryApiIT inherits this class and runs as an
    // integration test against the packaged binary, where CDI injection into the test is rejected.
    @BeforeAll
    void setUp() {
        final Path cache = Path.of("target/cache");
        if (!Files.exists(cache)) {
            return;
        }
        try (var paths = Files.walk(cache)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException ignored) {
                    // best effort, the cache is rebuilt on demand
                }
            });
        } catch (IOException ignored) {
            // best effort, the cache is rebuilt on demand
        }
    }

    @Test
    public void testBasicPom() {
        RestAssured.given().header("User-Agent", "m2e/unit-test").when()
                .get("/maven2/org/mvnpm/lit/3.1.2/lit-3.1.2.pom").then().log().all().and()
                .statusCode(Matchers.in(List.of(200, 404)));
    }

    @Test
    public void testStarDependencyPom() {
        RestAssured.given().header("User-Agent", "m2e/unit-test").when()
                .get("/maven2/org/mvnpm/at/types/codemirror/5.60.15/codemirror-5.60.15.pom").then()
                .log().all().and().statusCode(Matchers.in(List.of(200, 404)));
    }

    @Test
    public void testIgnoreBetaPom() {
        RestAssured.given().header("User-Agent", "m2e/unit-test").when()
                .get("/maven2/org/mvnpm/at/vaadin/tabs/24.3.8/vaadin-24.3.8.pom").then().log().all()
                .and().statusCode(Matchers.in(List.of(200, 404)));
    }

    @Test
    public void testNonCreatedPackageOtherFilesCanFail() {
        RestAssured.given().header("User-Agent", "m2e/unit-test").when()
                .get("/maven2/org/mvnpm/at/vaadin/tabs/24.3.8/vaadin-24.3.8.pom.sha1").then().log()
                .all().and().statusCode(Matchers.in(List.of(200, 404)));
        RestAssured.given().header("User-Agent", "m2e/unit-test").when()
                .get("/maven2/org/mvnpm/at/vaadin/tabs/24.3.8/vaadin-24.3.8.pom.md5").then().log().all()
                .and().statusCode(Matchers.in(List.of(200, 404)));
        RestAssured.given().header("User-Agent", "m2e/unit-test").when()
                .get("/maven2/org/mvnpm/at/vaadin/tabs/24.3.8/vaadin-24.3.8.jar.asc").then().log().all()
                .and().statusCode(Matchers.in(List.of(200, 404)));
        RestAssured.given().header("User-Agent", "m2e/unit-test").when()
                .get("/maven2/org/mvnpm/at/vaadin/tabs/24.3.8/vaadin-24.3.8.jar.asc.md5").then().log()
                .all().and().statusCode(Matchers.in(List.of(200, 404)));
    }

    @Test
    public void testPackageNotfound() {
        RestAssured.given().header("User-Agent", "m2e/unit-test").when()
                .get("/maven2/org/mvnpm/at/vaadin/tabs/23.3.40/vaadin-23.3.40.jar").then().log().all()
                .and().statusCode(404);
        RestAssured.given().header("User-Agent", "m2e/unit-test").when()
                .get("/maven2/org/mvnpm/at/vaadin/tas/23.2.40/vaain-23.2.40.jar").then().log().all()
                .and().statusCode(404);
        RestAssured.given().header("User-Agent", "m2e/unit-test").when()
                .get("/maven2/org/mvnpm/foo/foo-23.2.40.jar").then().log().all().and().statusCode(400);
    }

    @Test
    public void testInvalidPath() {
        RestAssured.given().header("User-Agent", "m2e/unit-test").when()
                .get("/maven2/org/mvnpm/foo/foo-23.2.40.jar").then().log().all().and().statusCode(400);
    }

}
