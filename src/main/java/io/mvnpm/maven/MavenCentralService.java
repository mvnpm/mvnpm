package io.mvnpm.maven;

import static io.mvnpm.Constants.CENTRAL_TMP_PREFIX;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;

import io.mvnpm.creator.FileType;
import io.mvnpm.creator.PackageFileLocator;
import io.mvnpm.maven.exceptions.MavenCentralRequestError;
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
    @Inject
    private PackageFileLocator packageFileLocator;

    private WebClient webClient() {
        return webClient.updateAndGet(wc -> wc == null ? WebClient.create(vertx) : wc);
    }

    public URI getUri(Name name, String version, String fileName) {
        String file = fileName;
        if (version != null) {
            file = "%s/%s".formatted(version, file);
        }
        return URI.create(
                (MAVEN_CENTRAL_REPO_URL + "/%s/%s/%s").formatted(name.mvnGroupIdPath(),
                        name.mvnArtifactId, file));
    }

    public Path downloadFromMavenCentral(Name name, String version, FileType type) {
        final HttpResponse<Buffer> response = getFromMavenCentral(name, version,
                packageFileLocator.getLocalFileName(type, name, version, Optional.empty())).await().atMost(
                        Duration.ofSeconds(15));
        try {
            Path downloaded = Files.createTempFile(CENTRAL_TMP_PREFIX + name.toGavString(version),
                    type.toString().toLowerCase());
            vertx.fileSystem().writeFile(downloaded.toString(), response.body()).await().atMost(Duration.ofSeconds(1));
            return downloaded;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    public Uni<HttpResponse<Buffer>> getFromMavenCentral(Name name, String version, FileType type) {
        return getFromMavenCentral(name, version, packageFileLocator.getLocalFileName(type, name, version, Optional.empty()));
    }

    public Uni<HttpResponse<Buffer>> getFromMavenCentral(Name name, String version, String fileName) {
        final URI uri = getUri(name, version, fileName);
        return webClient().getAbs(uri.toString()).send().map(Unchecked.function(r -> {
            if (r.statusCode() == 404) {
                throw new NotFoundInMavenCentralException(uri.toString());
            }
            if (r.statusCode() != 200) {
                throw new MavenCentralRequestError(uri.toString(), r.statusCode());
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
