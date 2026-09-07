package io.mvnpm.hosting.resources;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.mvnpm.hosting.NexusTestFixtures.NPM_REPOSITORY;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public final class NexusApiMockTestResource
        implements QuarkusTestResourceLifecycleManager {

    private static WireMockServer server;

    @Override
    public Map<String, String> start() {
        server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();

        final String baseUrl = server.baseUrl();
        return Map.ofEntries(
                Map.entry("mvnpm.custom.repository.url", baseUrl + "/repository/maven-public"),
                Map.entry("mvnpm.custom.repository.username", "test"),
                Map.entry("mvnpm.custom.repository.password", "test"),
                Map.entry("mvnpm.custom.repository.releases", "test-releases"),
                Map.entry("mvnpm.custom.repository.snapshots", "test-snapshots"),
                Map.entry("mvnpm.custom.repository.npm", NPM_REPOSITORY),
                Map.entry("quarkus.rest-client.repository.url", baseUrl),
                Map.entry("quarkus.rest-client.npm-registry.url", baseUrl + "/fallback"));
    }

    public static WireMockServer server() {
        if (server == null) {
            throw new IllegalStateException("WireMock server has not been started");
        }

        return server;
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }
}
