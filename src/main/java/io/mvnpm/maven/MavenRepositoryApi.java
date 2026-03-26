package io.mvnpm.maven;

import static io.mvnpm.Constants.HEADER_CACHE_CONTROL;
import static io.mvnpm.Constants.HEADER_CACHE_CONTROL_IMMUTABLE;

import java.util.Optional;

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
import io.mvnpm.mavencentral.sync.CentralSyncItem;
import io.mvnpm.mavencentral.sync.CentralSyncItemService;
import io.mvnpm.mavencentral.sync.CentralSyncService;
import io.mvnpm.npm.NpmRegistryFacade;
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
    NpmRegistryFacade npmRegistryFacade;

    @Inject
    MetadataService metadataService;

    @Inject
    CentralSyncService centralSyncService;

    @Inject
    CentralSyncItemService centralSyncItemService;

    @Inject
    PackageCreator packageCreator;

    @GET
    @Path("/org/mvnpm/{ga : (.+)?}/maven-metadata.xml")
    @NoCache
    @Produces(MediaType.APPLICATION_XML)
    public Response getMavenMetadata(@PathParam("ga") String ga) {
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
    @Path("/org/mvnpm/{ga : (.+)?}/maven-metadata.xml.sha1")
    @NoCache
    @Produces(MediaType.TEXT_PLAIN)
    public Response getMavenMetadataSha1(@PathParam("ga") String ga) {
        Name name = UrlPathParser.parseMavenMetaDataXml(ga);
        StreamingOutput streamingOutput = metadataService.getMetadataSha1(name);
        return Response.ok(streamingOutput).build();
    }

    @GET
    @Path("/org/mvnpm/{ga : (.+)?}/maven-metadata.xml.md5")
    @NoCache
    @Produces(MediaType.TEXT_PLAIN)
    public Response getMavenMetadataMd5(@PathParam("ga") String ga) {
        Name name = UrlPathParser.parseMavenMetaDataXml(ga);
        StreamingOutput streamingOutput = metadataService.getMetadataMd5(name);
        return Response.ok(streamingOutput).build();
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}/package.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPackageJson(@PathParam("gavt") String gavt) {
        NameVersion nameVersion = UrlPathParser.parseMavenFile(gavt + "/package.json");
        if (nameVersion.name().isInternal()) {
            return Response.ok()
                    .header(HEADER_CACHE_CONTROL, HEADER_CACHE_CONTROL_IMMUTABLE)
                    .build(); // TODO: Can we return this in some format ?
        } else {
            return Response.ok(npmRegistryFacade.getPackage(nameVersion.name().npmFullName, nameVersion.version()))
                    .header(HEADER_CACHE_CONTROL, HEADER_CACHE_CONTROL_IMMUTABLE)
                    .build();
        }
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}/importmap.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImportMap(@PathParam("gavt") String gavt) {
        NameVersion nameVersion = UrlPathParser.parseMavenFile(gavt + "/importmap.json");
        return Response.ok(mavenRepositoryService.getImportMap(nameVersion))
                .header(HEADER_CACHE_CONTROL, HEADER_CACHE_CONTROL_IMMUTABLE)
                .build();
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.pom")
    @Produces(MediaType.APPLICATION_XML)
    public Response getPom(@PathParam("gavt") String gavt) {
        NameVersion nv = UrlPathParser.parseMavenFile(gavt + ".pom");
        return resolveAndStream(nv, FileType.pom, Optional.empty(), mavenRepositoryService::getPath);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.pom.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPomSha1(@PathParam("gavt") String gavt) {
        NameVersion nv = UrlPathParser.parseMavenFile(gavt + ".pom.sha1");
        return resolveAndStream(nv, FileType.pom, Optional.of(Constants.DOT_SHA1), mavenRepositoryService::getSha1);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.pom.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPomMd5(@PathParam("gavt") String gavt) {
        NameVersion nv = UrlPathParser.parseMavenFile(gavt + ".pom.mp5");
        return resolveAndStream(nv, FileType.pom, Optional.of(Constants.DOT_MD5), mavenRepositoryService::getMd5);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.pom.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPomAsc(@PathParam("gavt") String gavt) {
        NameVersion nv = UrlPathParser.parseMavenFile(gavt + ".pom.asc");
        return resolveAndStream(nv, FileType.pom, Optional.of(Constants.DOT_ASC), mavenRepositoryService::getAsc);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.jar")
    @Produces("application/java-archive")
    public Response getJar(@PathParam("gavt") String gavt) {
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.jar, Optional.empty(), mavenRepositoryService::getPath);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.jar.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJarSha1(@PathParam("gavt") String gavt) {
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.jar, Optional.of(Constants.DOT_SHA1), mavenRepositoryService::getSha1);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.jar.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJarMd5(@PathParam("gavt") String gavt) {
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.jar, Optional.of(Constants.DOT_MD5), mavenRepositoryService::getMd5);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.jar.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJarAsc(@PathParam("gavt") String gavt) {
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.jar, Optional.of(Constants.DOT_ASC), mavenRepositoryService::getAsc);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-sources.jar")
    @Produces("application/java-archive")
    public Response getSourcesJar(@PathParam("gavt") String gavt) {
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.source, Optional.empty(), mavenRepositoryService::getPath);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-sources.jar.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getSourcesJarSha1(@PathParam("gavt") String gavt) {
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.source, Optional.of(Constants.DOT_SHA1), mavenRepositoryService::getSha1);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-sources.jar.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getSourcesJarMd5(@PathParam("gavt") String gavt) {
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.source, Optional.of(Constants.DOT_MD5), mavenRepositoryService::getMd5);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-sources.jar.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getSourcesJarAsc(@PathParam("gavt") String gavt) {
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.source, Optional.of(Constants.DOT_ASC), mavenRepositoryService::getAsc);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-javadoc.jar")
    @Produces("application/java-archive")
    public Response getJavadocJar(@PathParam("gavt") String gavt) {
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.javadoc, Optional.empty(), mavenRepositoryService::getPath);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-javadoc.jar.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJavadocJarSha1(@PathParam("gavt") String gavt) {
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.javadoc, Optional.of(Constants.DOT_SHA1), mavenRepositoryService::getSha1);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-javadoc.jar.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJavadocJarMd5(@PathParam("gavt") String gavt) {
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.javadoc, Optional.of(Constants.DOT_MD5), mavenRepositoryService::getMd5);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-javadoc.jar.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJavadocJarAsc(@PathParam("gavt") String gavt) {
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.javadoc, Optional.of(Constants.DOT_ASC), mavenRepositoryService::getAsc);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.tgz")
    @Produces("application/gzip")
    public Response getTgz(@PathParam("gavt") String gavt) {
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.tgz, Optional.empty(), mavenRepositoryService::getPath);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.tgz.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getTgzSha1(@PathParam("gavt") String gavt) {
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.tgz, Optional.of(Constants.DOT_SHA1), mavenRepositoryService::getSha1);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.tgz.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getTgzMd5(@PathParam("gavt") String gavt) {
        NameVersion nv = UrlPathParser.parseMavenFile(gavt);
        return resolveAndStream(nv, FileType.tgz, Optional.of(Constants.DOT_MD5), mavenRepositoryService::getMd5);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.tgz.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getTgzAsc(@PathParam("gavt") String gavt) {
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
        CentralSyncItem item = centralSyncService
                .checkReleaseInDbAndCentral(fullName.mvnGroupId, fullName.mvnArtifactId, version, type.triggerSync());
        if (item.alreadyReleased()) {
            throw packageCreator.newPackageAlreadySyncedException(fullName, version, type, dotSigned);
        }
        try {
            return streamPath(resolver.resolve(fullName, version, type));
        } catch (GetPackageException e) {
            if (e.isPermanentlyUnavailable()) {
                Log.warnf("Package permanently unavailable on NPM, cleaning up sync item: %s:%s:%s — %s",
                        fullName.mvnGroupId, fullName.mvnArtifactId, version, e.getMessage());
                centralSyncItemService.delete(item);
            }
            throw e;
        } catch (InvalidVersionException e) {
            Log.warnf("Invalid version, cleaning up sync item: %s:%s:%s — %s",
                    fullName.mvnGroupId, fullName.mvnArtifactId, version, e.getVersion());
            centralSyncItemService.delete(item);
            throw e;
        }
    }

    private Response streamPath(java.nio.file.Path path) {
        StreamingOutput streamingOutput = FileUtil.toStreamingOutput(path);
        return Response.ok(streamingOutput)
                .header(HEADER_CACHE_CONTROL, HEADER_CACHE_CONTROL_IMMUTABLE)
                .build();
    }
}
