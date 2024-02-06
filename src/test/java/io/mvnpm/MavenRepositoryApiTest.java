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
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;

@QuarkusTest
public class MavenRepositoryApiTest {

    @Test
    public void testBasicPom() {
        RestAssured.given().header("User-Agent", "m2e/unit-test")
                .when().get("/maven2/org/mvnpm/lit/2.4.0/lit-2.4.0.pom")
                .then().log().all().and()
                .statusCode(200);
    }

    @Test
    public void testStarDependencyPom() {
        RestAssured.given().header("User-Agent", "m2e/unit-test")
                .when().get("/maven2/org/mvnpm/at/types/codemirror/5.60.5/codemirror-5.60.5.pom")
                .then().log().all().and()
                .statusCode(200);
    }

    @Test
    public void testIgnoreBetaPom() {
        RestAssured.given().header("User-Agent", "m2e/unit-test")
                .when().get("/maven2/org/mvnpm/at/vaadin/tabs/23.2.3/vaadin-23.2.3.pom")
                .then().log().all().and()
                .statusCode(200);
    }

    @Test
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
    public void testComposite() throws IOException {
        RestAssuredConfig config = RestAssured.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam(CoreConnectionPNames.SO_TIMEOUT, 300000));
        final byte[] jar = RestAssured.given().header("User-Agent", "m2e/unit-test")
                .config(config)
                .when().get("/maven2/org/mvnpm/at/mvnpm/vaadin-webcomponents/24.2.5/vaadin-webcomponents-24.2.5.jar")
                .then()
                .statusCode(200)
                .extract().asByteArray();
        final Path tempFile = Files.createTempFile("vaadin-webcomponents-24.2.5", ".jar");
        final Path nodeModules = Files.createTempDirectory("node_modules");
        Files.write(tempFile, jar);
        WebDepsInstaller.install(nodeModules,
                List.of(WebDependency.of("vaadin-webcomponents", tempFile, WebDependency.WebDependencyType.MVNPM)));
        final MvnpmInfo mvnpmInfo = readMvnpmInfo(getMvnpmInfoPath(nodeModules));
        checkNodeModulesDir(nodeModules, mvnpmInfo);
        assertEquals(1, mvnpmInfo.installed().size());
        final MvnpmInfo.InstalledDependency installedVaadin = mvnpmInfo.installed().stream()
                .filter(installedDependency -> installedDependency.id().equals("vaadin-webcomponents"))
                .findFirst()
                .get();
        assertEquals(55, installedVaadin.dirs().size());
    }

    private void checkNodeModulesDir(Path nodeModules, MvnpmInfo mvnpmInfo) {
        final List<String> dirs = mvnpmInfo.installed().stream().flatMap(i -> i.dirs().stream()).toList();
        for (String dir : dirs) {
            final Path packageJson = nodeModules.resolve(dir).resolve("package.json");
            assertTrue(packageJson.toFile().exists(), "package.json should exist in " + packageJson);
        }
    }

}
