package io.mvnpm;

import static io.mvnpm.esbuild.install.WebDepsInstaller.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.apache.http.params.CoreConnectionPNames;
import org.junit.jupiter.api.Test;

import io.mvnpm.esbuild.install.MvnpmInfo;
import io.mvnpm.esbuild.install.WebDepsInstaller;
import io.mvnpm.esbuild.model.WebDependency;
import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;

@QuarkusTest
public class MavenRepositoryApiTest {

    @Test
    public void testBasicPom() {
        RestAssured.given().header("User-Agent", "m2e/unit-test")
                .when().get("/maven2/org/mvnpm/lit/3.1.2/lit-3.1.2.pom")
                .then().log().all().and()
                .statusCode(200);
    }

    @Test
    public void testStarDependencyPom() {
        RestAssured.given().header("User-Agent", "m2e/unit-test")
                .when().get("/maven2/org/mvnpm/at/types/codemirror/5.60.15/codemirror-5.60.15.pom")
                .then().log().all().and()
                .statusCode(200);
    }

    @Test
    public void testIgnoreBetaPom() {
        RestAssured.given().header("User-Agent", "m2e/unit-test")
                .when().get("/maven2/org/mvnpm/at/vaadin/tabs/24.3.8/vaadin-24.3.8.pom")
                .then().log().all().and()
                .statusCode(200);
    }

    @Test
    @DisabledOnIntegrationTest
    public void testFileTooLong() throws IOException {
        final byte[] jar = RestAssured.given().header("User-Agent", "m2e/unit-test")
                .when().get("/maven2/org/mvnpm/at/ui5/webcomponents-fiori/1.20.1/webcomponents-fiori-1.20.1.jar")
                .then()
                .statusCode(200)
                .extract().asByteArray();
        final Path tempFile = Files.createTempFile("webcomponents-fiori-1.20.1", ".jar");
        final Path nodeModules = Files.createTempDirectory("node_modules");
        Files.write(tempFile, jar);
        WebDepsInstaller.install(nodeModules,
                List.of(WebDependency.of("webcomponents-fiori-1.20.1", tempFile, WebDependency.WebDependencyType.MVNPM)));
        final MvnpmInfo mvnpmInfo = readMvnpmInfo(getMvnpmInfoPath(nodeModules));
        checkNodeModulesDir(nodeModules, mvnpmInfo);
        assertEquals(1, mvnpmInfo.installed().size());
        assertEquals(Set.of(new MvnpmInfo.InstalledDependency("webcomponents-fiori-1.20.1",
                List.of("@ui5/webcomponents-fiori"))), mvnpmInfo.installed());
    }

    @Test
    @DisabledOnIntegrationTest
    public void testComposite() throws IOException {
        RestAssuredConfig config = RestAssured.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam(CoreConnectionPNames.SO_TIMEOUT, 300000));
        final byte[] jar = RestAssured.given().header("User-Agent", "m2e/unit-test")
                .config(config)
                .when().get("/maven2/org/mvnpm/at/mvnpm/lit/3.1.2/lit-3.1.2.jar")
                .then()
                .statusCode(200)
                .extract().asByteArray();
        final Path tempFile = Files.createTempFile("lit-3.1.2", ".jar");
        final Path nodeModules = Files.createTempDirectory("node_modules");
        Files.write(tempFile, jar);
        WebDepsInstaller.install(nodeModules,
                List.of(WebDependency.of("lit", tempFile, WebDependency.WebDependencyType.MVNPM)));
        final MvnpmInfo mvnpmInfo = readMvnpmInfo(getMvnpmInfoPath(nodeModules));
        checkNodeModulesDir(nodeModules, mvnpmInfo);
        assertEquals(1, mvnpmInfo.installed().size());
        final MvnpmInfo.InstalledDependency installedLit = mvnpmInfo.installed().stream()
                .filter(installedDependency -> installedDependency.id().equals("lit"))
                .findFirst()
                .get();
        assertEquals(6, installedLit.dirs().size());
    }

    private void checkNodeModulesDir(Path nodeModules, MvnpmInfo mvnpmInfo) {
        final List<String> dirs = mvnpmInfo.installed().stream().flatMap(i -> i.dirs().stream()).toList();
        for (String dir : dirs) {
            final Path packageJson = nodeModules.resolve(dir).resolve("package.json");
            assertTrue(packageJson.toFile().exists(), "package.json should exist in " + packageJson);
        }
    }

}
