package io.mvnpm.maven;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;

import io.mvnpm.maven.exceptions.NotFoundInMavenCentralException;
import io.mvnpm.npm.model.Name;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

@Singleton
public class MavenCentralService {

    public static final String MAVEN_CENTRAL_REPO_URL = "https://repo1.maven.org/maven2";

    @Inject
    private Vertx vertx;

    private final AtomicReference<WebClient> webClient = new AtomicReference<>();

    private WebClient webClient() {
        return webClient.updateAndGet(webClient -> webClient == null ? WebClient.create(vertx) : webClient);
    }

    public Uni<HttpResponse<Buffer>> getFromMavenCentral(Name name, String version, String fileName) {
        String file = fileName;
        if (version != null) {
            file = "%s/%s".formatted(version, file);
        }
        final URI uri = URI.create(
                (MAVEN_CENTRAL_REPO_URL + "/%s/%s/%s").formatted(name.mvnGroupIdPath(),
                        name.mvnArtifactId, file));
        return webClient().getAbs(uri.toString()).send().map(Unchecked.function(r -> {
            if (r.statusCode() != 200) {
                throw new NotFoundInMavenCentralException(uri.toString());
            }
            return r;
        }));
    }

    public Uni<Response> proxyMavenRequest(Name name, String version, String fileName) {
        return getFromMavenCentral(name, version, fileName)
                .onItem().transform(response -> {
                    if (response.statusCode() == 200) {
                        final Response.ResponseBuilder builder = Response.ok(response.bodyAsBuffer());
                        for (Map.Entry<String, String> header : response.headers()) {
                            builder.header(header.getKey(), header.getValue());
                        }
                        return builder.build();
                    } else {
                        return Response.status(response.statusCode())
                                .entity(response.bodyAsString())
                                .build();
                    }
                });
    }
}
