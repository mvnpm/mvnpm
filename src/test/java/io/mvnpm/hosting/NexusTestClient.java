package io.mvnpm.hosting;

import static io.restassured.RestAssured.given;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public final class NexusTestClient {

    private final String baseUrl;
    private final String username;
    private final String password;

    public NexusTestClient(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    public void createMavenHosted(String name, MavenVersionPolicy versionPolicy, MavenWritePolicy writePolicy) {
        if (exists(name)) {
            return;
        }

        String body = """
                {
                  "name": "%s",
                  "online": true,
                  "storage": {
                    "blobStoreName": "default",
                    "strictContentTypeValidation": true,
                    "writePolicy": "%s"
                  },
                  "maven": {
                    "versionPolicy": "%s",
                    "layoutPolicy": "STRICT",
                    "contentDisposition": "ATTACHMENT"
                  }
                }
                """.formatted(
                name,
                writePolicy.name().toLowerCase(),
                versionPolicy.name());

        post("/service/rest/v1/repositories/maven/hosted", body);
    }

    public void createNpmHosted(String name, MavenWritePolicy writePolicy) {
        if (exists(name)) {
            return;
        }

        String body = """
                {
                  "name": "%s",
                  "online": true,
                  "storage": {
                    "blobStoreName": "default",
                    "strictContentTypeValidation": true,
                    "writePolicy": "%s"
                  }
                }
                """.formatted(
                name,
                writePolicy.name().toLowerCase());

        post("/service/rest/v1/repositories/npm/hosted", body);
    }

    public void createMavenProxy(String name, String remoteUrl, MavenVersionPolicy versionPolicy) {
        if (exists(name)) {
            return;
        }

        String body = """
                {
                  "name": "%s",
                  "online": true,
                  "storage": {
                    "blobStoreName": "default",
                    "strictContentTypeValidation": true
                  },
                  "proxy": {
                    "remoteUrl": "%s",
                    "contentMaxAge": 1440,
                    "metadataMaxAge": 1440
                  },
                  "negativeCache": {
                    "enabled": true,
                    "timeToLive": 1440
                  },
                  "httpClient": {
                    "blocked": false,
                    "autoBlock": true
                  },
                  "maven": {
                    "versionPolicy": "%s",
                    "layoutPolicy": "STRICT",
                    "contentDisposition": "ATTACHMENT"
                  }
                }
                """.formatted(name, remoteUrl, versionPolicy.name());

        post("/service/rest/v1/repositories/maven/proxy", body);
    }

    public void createMavenGroup(String name, String... members) {
        if (exists(name)) {
            return;
        }

        String memberNames = java.util.Arrays.stream(members)
                .map(member -> "\"" + member + "\"")
                .collect(java.util.stream.Collectors.joining(","));

        String body = """
                {
                  "name": "%s",
                  "online": true,
                  "storage": {
                    "blobStoreName": "default",
                    "strictContentTypeValidation": true
                  },
                  "group": {
                    "memberNames": [%s]
                  }
                }
                """.formatted(name, memberNames);

        post("/service/rest/v1/repositories/maven/group", body);
    }

    public void uploadNpmPackage(
            String repository,
            String fileName,
            byte[] tgz) {

        final Response response = given()
                .baseUri(baseUrl)
                .auth()
                .preemptive()
                .basic(username, password)
                .queryParam("repository", repository)
                .multiPart(
                        "npm.asset",
                        fileName,
                        tgz,
                        "application/gzip")
                .when()
                .post("/service/rest/v1/components");

        if (response.statusCode() != 204) {
            throw new IllegalStateException(
                    "Could not upload npm fixture "
                            + fileName
                            + " to repository "
                            + repository
                            + ": HTTP "
                            + response.statusCode()
                            + "\nResponse:\n"
                            + response.asString());
        }
    }

    public void awaitNpmPackage(
            String repository,
            String scope,
            String name,
            String version) {

        final String nexusScope = nexusNpmScope(scope);

        final long deadline = System.nanoTime()
                + Duration.ofSeconds(30).toNanos();

        Response lastSearchResponse = null;

        while (System.nanoTime() < deadline) {
            lastSearchResponse = given()
                    .baseUri(baseUrl)
                    .auth()
                    .preemptive()
                    .basic(username, password)
                    .queryParam("repository", repository)
                    .queryParam("format", "npm")
                    .queryParam("npm.scope", nexusScope)
                    .queryParam("name", name)
                    .queryParam("version", version)
                    .when()
                    .get("/service/rest/v1/search/assets");

            if (lastSearchResponse.statusCode() == 200) {
                final List<?> items = lastSearchResponse
                        .jsonPath()
                        .getList("items");

                if (items != null && !items.isEmpty()) {
                    return;
                }
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                throw new IllegalStateException(
                        "Interrupted while waiting for "
                                + scope
                                + "/"
                                + name
                                + "@"
                                + version,
                        e);
            }
        }

        final Response components = given()
                .baseUri(baseUrl)
                .auth()
                .preemptive()
                .basic(username, password)
                .queryParam("repository", repository)
                .when()
                .get("/service/rest/v1/components");

        throw new IllegalStateException(
                "Nexus did not make npm fixture searchable: "
                        + scope
                        + "/"
                        + name
                        + "@"
                        + version
                        + "\nNexus scope used for search: "
                        + nexusScope
                        + "\n\nLast search response: HTTP "
                        + (lastSearchResponse == null
                                ? "<none>"
                                : lastSearchResponse.statusCode())
                        + "\n"
                        + (lastSearchResponse == null
                                ? ""
                                : lastSearchResponse.asString())
                        + "\n\nComponents currently stored in repository:\n"
                        + components.asString());
    }

    public void acceptEulaIfRequired() {
        final Response response = given()
                .baseUri(baseUrl)
                .auth()
                .preemptive()
                .basic(username, password)
                .accept(ContentType.JSON)
                .when()
                .get("/service/rest/v1/system/eula");

        // Older OSS versions, such as 3.76.1, do not expose this endpoint.
        if (response.statusCode() == 404
                || response.statusCode() == 405) {
            return;
        }

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Could not query Nexus EULA state: HTTP "
                            + response.statusCode()
                            + "\n"
                            + response.asString());
        }

        final Boolean accepted = response.jsonPath().getBoolean("accepted");

        if (Boolean.TRUE.equals(accepted)) {
            return;
        }

        final String disclaimer = response.jsonPath().getString("disclaimer");

        final Response acceptResponse = given()
                .baseUri(baseUrl)
                .auth()
                .preemptive()
                .basic(username, password)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "accepted", true,
                        "disclaimer", disclaimer))
                .when()
                .post("/service/rest/v1/system/eula");

        if (acceptResponse.statusCode() < 200
                || acceptResponse.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Could not accept Nexus EULA: HTTP "
                            + acceptResponse.statusCode()
                            + "\n"
                            + acceptResponse.asString());
        }
    }

    public boolean exists(String name) {
        return given()
                .baseUri(baseUrl)
                .auth()
                .preemptive()
                .basic(username, password)
                .when()
                .get("/service/rest/v1/repositories/" + name)
                .statusCode() == 200;
    }

    public void delete(String name) {
        given()
                .baseUri(baseUrl)
                .auth()
                .preemptive()
                .basic(username, password)
                .when()
                .delete("/service/rest/v1/repositories/" + name)
                .then()
                .statusCode(204);
    }

    private void post(String endpoint, String body) {
        final Response response = given()
                .baseUri(baseUrl)
                .auth()
                .preemptive()
                .basic(username, password)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(body)
                .when()
                .post(endpoint);

        if (response.statusCode() != 201) {
            throw new IllegalStateException(
                    "Nexus request failed: POST "
                            + endpoint
                            + " -> HTTP "
                            + response.statusCode()
                            + "\nResponse:\n"
                            + response.asString()
                            + "\nRequest body:\n"
                            + body);
        }
    }

    private String nexusNpmScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return null;
        }

        return scope.startsWith("@")
                ? scope.substring(1)
                : scope;
    }
}
