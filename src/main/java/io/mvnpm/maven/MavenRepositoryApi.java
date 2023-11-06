package io.mvnpm.maven;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.mvnpm.composite.CompositeService;
import io.mvnpm.file.FileType;
import io.mvnpm.file.ImportMapUtil;
import io.mvnpm.file.metadata.MetadataAndHash;
import io.mvnpm.file.metadata.MetadataClient;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.Package;

/**
 * The maven repository endpoint
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@Path("/maven2")
public class MavenRepositoryApi {

    @Inject
    MavenRespositoryService mavenRespositoryService;

    @Inject
    NpmRegistryFacade npmRegistryFacade;

    @Inject
    MetadataClient metadataClient;

    @Inject
    CompositeService compositeService;

    @GET
    @Path("/org/mvnpm/{ga : (.+)?}/maven-metadata.xml")
    @Produces(MediaType.APPLICATION_XML)
    public Response getMavenMetadata(@PathParam("ga") String ga) {
        try {
            Name name = UrlPathParser.parseMavenMetaDataXml(ga);
            MetadataAndHash mah = metadataClient.getMetadataAndHash(name);
            return Response.ok(mah.data()).build();
        } catch (WebApplicationException wae) {
            return wae.getResponse();
        } catch (Throwable t) {
            return Response.serverError().header("reason", t.getMessage()).build();
        }
    }

    @GET
    @Path("/org/mvnpm/{ga : (.+)?}/maven-metadata.xml.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getMavenMetadataSha1(@PathParam("ga") String ga) {
        Name name = UrlPathParser.parseMavenMetaDataXml(ga);
        MetadataAndHash mah = metadataClient.getMetadataAndHash(name);
        return Response.ok(mah.sha1()).build();
    }

    @GET
    @Path("/org/mvnpm/{ga : (.+)?}/maven-metadata.xml.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getMavenMetadataMd5(@PathParam("ga") String ga) {
        Name name = UrlPathParser.parseMavenMetaDataXml(ga);
        MetadataAndHash mah = metadataClient.getMetadataAndHash(name);
        return Response.ok(mah.md5()).build();
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}/package.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPackageJson(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt + "/package.json");
        if (nameVersionType.name().isInternal()) {
            return Response.ok().build(); // TODO: Can we return this in some format ?
        } else {
            return Response.ok(npmRegistryFacade.getPackage(nameVersionType.name().npmFullName, nameVersionType.version()))
                    .build();
        }
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}/importmap.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImportMap(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt + "/importmap.json");
        if (nameVersionType.name().isInternal()) {
            byte[] importMap = compositeService.getImportMap(nameVersionType.name(), nameVersionType.version());
            return Response.ok(importMap).build();
        } else {
            Package npmPackage = npmRegistryFacade.getPackage(nameVersionType.name().npmFullName, nameVersionType.version());
            return Response.ok(ImportMapUtil.createImportMap(npmPackage)).build();
        }
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.pom")
    @Produces(MediaType.APPLICATION_XML)
    public Response getPom(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt + ".pom");
        return toResponse(nameVersionType.name(), nameVersionType.version(), FileType.pom);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.pom.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPomSha1(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt + ".pom.sha1");
        return toSha1Response(nameVersionType.name(), nameVersionType.version(), FileType.pom);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.pom.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPomMd5(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt + ".pom.mp5");
        return toMd5Response(nameVersionType.name(), nameVersionType.version(), FileType.pom);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.pom.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPomAsc(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt + ".pom.asc");
        return toAscResponse(nameVersionType.name(), nameVersionType.version(), FileType.pom);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.jar")
    @Produces("application/java-archive")
    public Response getJar(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toResponse(nameVersionType.name(), nameVersionType.version(), FileType.jar);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.jar.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJarSha1(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toSha1Response(nameVersionType.name(), nameVersionType.version(), FileType.jar);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.jar.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJarMd5(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toMd5Response(nameVersionType.name(), nameVersionType.version(), FileType.jar);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.jar.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJarAsc(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toAscResponse(nameVersionType.name(), nameVersionType.version(), FileType.jar);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-sources.jar")
    @Produces("application/java-archive")
    public Response getSourcesJar(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toResponse(nameVersionType.name(), nameVersionType.version(), FileType.source);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-sources.jar.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getSourcesJarSha1(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toSha1Response(nameVersionType.name(), nameVersionType.version(), FileType.source);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-sources.jar.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getSourcesJarMd5(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toMd5Response(nameVersionType.name(), nameVersionType.version(), FileType.source);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-sources.jar.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getSourcesJarAsc(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toAscResponse(nameVersionType.name(), nameVersionType.version(), FileType.source);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-javadoc.jar")
    @Produces("application/java-archive")
    public Response getJavadocJar(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toResponse(nameVersionType.name(), nameVersionType.version(), FileType.javadoc);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-javadoc.jar.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJavadocJarSha1(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toSha1Response(nameVersionType.name(), nameVersionType.version(), FileType.javadoc);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-javadoc.jar.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJavadocJarMd5(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toMd5Response(nameVersionType.name(), nameVersionType.version(), FileType.javadoc);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-javadoc.jar.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJavadocJarAsc(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toAscResponse(nameVersionType.name(), nameVersionType.version(), FileType.javadoc);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.tgz")
    @Produces("application/gzip")
    public Response getTgz(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toResponse(nameVersionType.name(), nameVersionType.version(), FileType.tgz);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.tgz.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getTgzSha1(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toSha1Response(nameVersionType.name(), nameVersionType.version(), FileType.tgz);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.tgz.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getTgzMd5(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toMd5Response(nameVersionType.name(), nameVersionType.version(), FileType.tgz);
    }

    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.tgz.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getTgzAsc(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toAscResponse(nameVersionType.name(), nameVersionType.version(), FileType.tgz);
    }

    private Response toResponse(Name fullName, String version, FileType type) {
        byte[] file = mavenRespositoryService.getFile(fullName, version, type);
        return Response.ok(file).build();
    }

    private Response toSha1Response(Name fullName, String version, FileType type) {
        byte[] file = mavenRespositoryService.getSha1(fullName, version, type);
        return Response.ok(file).build();
    }

    private Response toMd5Response(Name fullName, String version, FileType type) {
        byte[] file = mavenRespositoryService.getMd5(fullName, version, type);
        return Response.ok(file).build();
    }

    private Response toAscResponse(Name fullName, String version, FileType type) {
        byte[] file = mavenRespositoryService.getAsc(fullName, version, type);
        return Response.ok(file).build();
    }
}
