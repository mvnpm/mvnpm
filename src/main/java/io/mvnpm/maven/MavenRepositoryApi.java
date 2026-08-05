package io.mvnpm.maven;

import static io.mvnpm.Constants.HEADER_CACHE_CONTROL;
import static io.mvnpm.Constants.HEADER_CACHE_CONTROL_IMMUTABLE;

import java.util.Optional;

import org.jboss.resteasy.reactive.NoCache;

import io.mvnpm.Constants;
import io.mvnpm.creator.FileType;
import io.mvnpm.creator.PackageCreator;
import io.mvnpm.creator.type.MetadataService;
import io.mvnpm.creator.utils.FileUtil;
import io.mvnpm.maven.api.Namespace;
import io.mvnpm.maven.api.SyncItem;
import io.mvnpm.maven.api.SyncItemService;
import io.mvnpm.maven.api.SyncService;
import io.mvnpm.npm.api.NpmFacade;
import io.mvnpm.npm.exceptions.GetPackageException;
import io.mvnpm.npm.model.Name;
import io.mvnpm.version.InvalidVersionException;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import org.jboss.resteasy.reactive.NoCache;

import io.mvnpm.Constants;
import io.mvnpm.creator.FileType;
import io.mvnpm.creator.PackageCreator;
import io.mvnpm.creator.type.MetadataService;
import io.mvnpm.creator.utils.FileUtil;
import io.mvnpm.creator.utils.UrlPathParser;
import io.mvnpm.maven.api.NameVersion;
import io.mvnpm.maven.sync.SyncItem;
import io.mvnpm.maven.sync.SyncItemService;
import io.mvnpm.maven.sync.SyncService;
import io.mvnpm.npm.api.NpmFacade;
import io.mvnpm.npm.exceptions.GetPackageException;
import io.mvnpm.npm.model.Name;
import io.mvnpm.version.InvalidVersionException;
import io.quarkus.logging.Log;

/**
 * The maven repository endpoint
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@Path("/maven2")
public class MavenRepositoryApi {

    @Inject
    MavenRepositoryService mavenRepositoryService;

    @Inject
    NpmFacade npmFacade;

    @Inject
    MetadataService metadataService;

    @Inject
    SyncService syncService;

    @Inject
    SyncItemService syncItemService;

    @Inject
    PackageCreator packageCreator;

    @Inject
    Namespace namespace;

    @GET
    @Path("/{namespace}/{ga : (.+)?}/maven-metadata.xml")
    @NoCache
    @Produces(MediaType.APPLICATION_XML)
    public Response getMavenMetadata(@PathParam("namespace") String namespace, @PathParam("ga") String ga) {
        this.namespace.check(namespace);
        Name name = UrlPathParser.parseMavenMetaDataXml(ga);
        try {
            StreamingOutput streamingOutput = FileUtil.toStreamingOutput(metadataService.getMetadataXml(name));
            return Response.ok(streamingOutput).build();
        } catch (WebApplicationException wae) {
            return wae.getResponse();
        } catch (Throwable t) {
            return Response.serverError().header("reason", t.getMessage()).build();
        }
    }

    @GET
    @Path("/{namespace}/{ga : (.+)?}/maven-metadata.xml.sha1")
    @NoCache
    @Produces(MediaType.TEXT_PLAIN)
    public Response getMavenMetadataSha1(@PathParam("namespace") String namespace, @PathParam("ga") String ga) {
        this.namespace.check(namespace);
        Name name = UrlPathParser.parseMavenMetaDataXml(ga);
        StreamingOutput streamingOutput = metadataService.getMetadataSha1(name);
        return Response.ok(streamingOutput).build();
    }

    @GET
    @Path("/{namespace}/{ga : (.+)?}/maven-metadata.xml.md5")
    @NoCache
    @Produces(MediaType.TEXT_PLAIN)
    public Response getMavenMetadataMd5(@PathParam("namespace") String namespace, @PathParam("ga") String ga) {
        this.namespace.check(namespace);
        Name name = UrlPathParser.parseMavenMetaDataXml(ga);
        StreamingOutput streamingOutput = metadataService.getMetadataMd5(name);
        return Response.ok(streamingOutput).build();
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}/package.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPackageJson(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nameVersion = UrlPathParser.parseMavenFile(gavt + "/package.json");
        if (nameVersion.name().isInternal()) {
            return Response.ok().header(HEADER_CACHE_CONTROL, HEADER_CACHE_CONTROL_IMMUTABLE).build(); // TODO: Can we return this in some format ?
        } else {
            return Response.ok(npmFacade.getPackage(nameVersion.name().npmFullName, nameVersion.version()))
                    .header(HEADER_CACHE_CONTROL, HEADER_CACHE_CONTROL_IMMUTABLE).build();
        }
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}/importmap.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImportMap(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nameVersion = UrlPathParser.parseMavenFile(gavt + "/importmap.json");
        return Response.ok(mavenRepositoryService.getImportMap(nameVersion))
                .header(HEADER_CACHE_CONTROL, HEADER_CACHE_CONTROL_IMMUTABLE).build();
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}.pom")
    @Produces(MediaType.APPLICATION_XML)
    public Response getPom(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt + ".pom");
        return resolveAndStream(nv, FileType.pom, Optional.empty(), mavenRepositoryService::getPath);
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}.pom.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPomSha1(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt + ".pom.sha1");
        return resolveAndStream(nv, FileType.pom, Optional.of(Constants.DOT_SHA1), mavenRepositoryService::getSha1);
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}.pom.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPomMd5(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt + ".pom.mp5");
        return resolveAndStream(nv, FileType.pom, Optional.of(Constants.DOT_MD5), mavenRepositoryService::getMd5);
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}.pom.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPomAsc(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt + ".pom.asc");
        return resolveAndStream(nv, FileType.pom, Optional.of(Constants.DOT_ASC), mavenRepositoryService::getAsc);
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}.jar")
    @Produces("application/java-archive")
    public Response getJar(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.jar, Optional.empty(), mavenRepositoryService::getPath);
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}.jar.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJarSha1(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.jar, Optional.of(Constants.DOT_SHA1), mavenRepositoryService::getSha1);
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}.jar.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJarMd5(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.jar, Optional.of(Constants.DOT_MD5), mavenRepositoryService::getMd5);
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}.jar.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJarAsc(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.jar, Optional.of(Constants.DOT_ASC), mavenRepositoryService::getAsc);
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}-sources.jar")
    @Produces("application/java-archive")
    public Response getSourcesJar(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.source, Optional.empty(), mavenRepositoryService::getPath);
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}-sources.jar.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getSourcesJarSha1(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.source, Optional.of(Constants.DOT_SHA1), mavenRepositoryService::getSha1);
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}-sources.jar.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getSourcesJarMd5(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.source, Optional.of(Constants.DOT_MD5), mavenRepositoryService::getMd5);
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}-sources.jar.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getSourcesJarAsc(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.source, Optional.of(Constants.DOT_ASC), mavenRepositoryService::getAsc);
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}-javadoc.jar")
    @Produces("application/java-archive")
    public Response getJavadocJar(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.javadoc, Optional.empty(), mavenRepositoryService::getPath);
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}-javadoc.jar.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJavadocJarSha1(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.javadoc, Optional.of(Constants.DOT_SHA1), mavenRepositoryService::getSha1);
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}-javadoc.jar.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJavadocJarMd5(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.javadoc, Optional.of(Constants.DOT_MD5), mavenRepositoryService::getMd5);
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}-javadoc.jar.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJavadocJarAsc(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.javadoc, Optional.of(Constants.DOT_ASC), mavenRepositoryService::getAsc);
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}.tgz")
    @Produces("application/gzip")
    public Response getTgz(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.tgz, Optional.empty(), mavenRepositoryService::getPath);
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}.tgz.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getTgzSha1(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.tgz, Optional.of(Constants.DOT_SHA1), mavenRepositoryService::getSha1);
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}.tgz.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getTgzMd5(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.tgz, Optional.of(Constants.DOT_MD5), mavenRepositoryService::getMd5);
    }

    @GET
    @Path("/{namespace}/{gavt : (.+)?}.tgz.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getTgzAsc(@PathParam("namespace") String namespace, @PathParam("gavt") String gavt) {
        this.namespace.check(namespace);
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.tgz, Optional.of(Constants.DOT_ASC), mavenRepositoryService::getAsc);
    }

    @FunctionalInterface
    interface PathResolver {
        java.nio.file.Path resolve(Name name, String version, FileType type);
    }

    Response resolveAndStream(NameVersion nv, FileType type, Optional<String> dotSigned, PathResolver resolver) {
        Name fullName = nv.name();
        String version = nv.version();
        SyncItem item = syncService.checkReleaseInDbAndRepo(fullName.mvnGroupId, fullName.mvnArtifactId, version,
                type.triggerSync());
        if (item.alreadyReleased()) {
            throw packageCreator.newPackageAlreadySyncedException(fullName, version, type, dotSigned);
        }
        try {
            return streamPath(resolver.resolve(fullName, version, type));
        } catch (GetPackageException e) {
            if (e.isPermanentlyUnavailable()) {
                Log.warnf("Package permanently unavailable on NPM, cleaning up sync item: %s:%s:%s — %s",
                        fullName.mvnGroupId, fullName.mvnArtifactId, version, e.getMessage());
                syncItemService.delete(item);
            }
            throw e;
        } catch (InvalidVersionException e) {
            Log.warnf("Invalid version, cleaning up sync item: %s:%s:%s — %s", fullName.mvnGroupId,
                    fullName.mvnArtifactId, version, e.getVersion());
            syncItemService.delete(item);
            throw e;
        }
    }

    private Response streamPath(java.nio.file.Path path) {
        StreamingOutput streamingOutput = FileUtil.toStreamingOutput(path);
        return Response.ok(streamingOutput).header(HEADER_CACHE_CONTROL, HEADER_CACHE_CONTROL_IMMUTABLE).build();
    }
}
