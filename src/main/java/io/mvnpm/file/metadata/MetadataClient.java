package io.mvnpm.file.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.StreamingOutput;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.mvnpm.Constants;
import io.mvnpm.composite.CompositeService;
import io.mvnpm.file.FileStore;
import io.mvnpm.file.FileUtil;
import io.mvnpm.maven.MavenCentralService;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.Project;
import io.mvnpm.version.InvalidVersionException;
import io.mvnpm.version.Version;
import io.quarkus.logging.Log;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;

/**
 * Creates a maven-metadata.xml from the NPM Project
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class MetadataClient {
    private final MetadataXpp3Writer metadataXpp3Writer = new MetadataXpp3Writer();
    private final MetadataXpp3Reader metadataXpp3Reader = new MetadataXpp3Reader();

    @Inject
    NpmRegistryFacade npmRegistryFacade;

    @Inject
    CompositeService compositeService;

    @Inject
    MavenCentralService mavenCentralService;

    @Inject
    FileStore fileStore;

    @ConfigProperty(name = "mvnpm.metadata-timeout.minutes")
    int timeout;

    public Metadata getMetadata(Name name) {
        final Path metadataXml = getMetadataXml(name);
        try (InputStream is = Files.newInputStream(metadataXml)) {
            return metadataXpp3Reader.read(is);
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException(e);
        }

    }

    public Path getMetadataXml(Name name) {
        Path localFilePath = fileStore.getLocalMetadataXmlFullPath(name);
        // Create if does not exist.
        if (!Files.exists(localFilePath) || !Files.isRegularFile(localFilePath) || isOlderThanTimeout(localFilePath)) {
            createDir(localFilePath);
            if (name.isInternal()) {
                final Buffer buffer = mavenCentralService.getFromMavenCentral(name, null, Constants.MAVEN_METADATA_XML)
                        .map(HttpResponse::bodyAsBuffer)
                        .await().atMost(Duration.ofSeconds(10));
                FileUtil.writeAtomic(localFilePath, buffer.getBytes());
            } else {
                try (StringWriter stringWriter = new StringWriter()) {
                    Metadata metadata = buildMetadata(name);
                    metadataXpp3Writer.write(stringWriter, metadata);
                    FileUtil.writeAtomic(localFilePath, stringWriter.toString());
                    FileUtil.createSha1(localFilePath, true);
                    FileUtil.createMd5(localFilePath, true);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }

        }
        return localFilePath;
    }

    public StreamingOutput getMetadataSha1(Name name) {

        Path localSha1Path = fileStore.getLocalMetadataXmlSha1FullPath(name);

        // Create if if does not exist.
        if (!Files.exists(localSha1Path) || !Files.isRegularFile(localSha1Path)) {
            createDir(localSha1Path);
            Path localFilePath = fileStore.getLocalMetadataXmlFullPath(name);
            FileUtil.createSha1(localFilePath, true);
        }
        return FileUtil.toStreamingOutput(localSha1Path);
    }

    public StreamingOutput getMetadataMd5(Name name) {

        Path localMd5Path = fileStore.getLocalMetadataXmlMd5FullPath(name);

        // Create if if does not exist.
        if (!Files.exists(localMd5Path) || !Files.isRegularFile(localMd5Path)) {
            Path localFilePath = fileStore.getLocalMetadataXmlFullPath(name);
            FileUtil.createMd5(localFilePath, true);
        }
        return FileUtil.toStreamingOutput(localMd5Path);
    }

    private Metadata buildMetadata(Name name) {
        Metadata metadata = new Metadata();
        metadata.setGroupId(name.mvnGroupId);
        metadata.setArtifactId(name.mvnArtifactId);
        metadata.setVersioning(getVersioning(name));
        return metadata;

    }

    private Versioning getVersioning(Name name) {
        return getNpmVersioning(name);
    }

    private Versioning getNpmVersioning(Name name) {
        Project project = npmRegistryFacade.getProject(name.npmFullName);
        Versioning versioning = new Versioning();
        String latest = getLatest(project);
        versioning.setLatest(latest);
        versioning.setRelease(latest);

        for (String version : project.versions()) {
            try {
                Version v = Version.fromString(version);
                if (!versioning.getVersions().contains(v.toString())) {
                    // Ignore pre release
                    if (v.qualifier() == null) {
                        versioning.addVersion(v.toString());
                    }
                }
            } catch (InvalidVersionException ive) {
                Log.warn("Ignoring version [" + ive.getVersion() + "] for " + name.displayName);
            }
        }

        Map<String, String> time = project.time();
        if (time != null && time.containsKey(MODIFIED)) {
            String dateTime = time.get(MODIFIED);
            // 2022-07-20T09:14:55.450Z
            dateTime = dateTime.replaceAll(Constants.HYPHEN, Constants.EMPTY);
            // 20220720T09:14:55.450Z
            dateTime = dateTime.replaceAll("T", Constants.EMPTY);
            // 2022072009:14:55.450Z
            dateTime = dateTime.replaceAll(Constants.DOUBLE_POINT, Constants.EMPTY);
            // 20220720091455.450Z
            if (dateTime.contains(Constants.DOT)) {
                int i = dateTime.indexOf(Constants.DOT);
                dateTime = dateTime.substring(0, i);
            }
            versioning.setLastUpdated(dateTime);
        } else {
            String timestamp = new SimpleDateFormat(TIME_STAMP_FORMAT).format(new Date());
            versioning.setLastUpdated(timestamp);
        }
        return versioning;
    }

    private String getLatest(Project p) {
        return p.distTags().latest();
    }

    private void createDir(Path localFilePath) {
        Path parentDir = localFilePath.getParent();
        if (!Files.exists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }

    private boolean isOlderThanTimeout(Path p) {
        try {
            FileTime t = Files.getLastModifiedTime(p);
            Instant fileInstant = t.toInstant();
            Instant now = Clock.systemDefaultZone().instant();
            Duration difference = Duration.between(fileInstant, now);
            long minutes = difference.toMinutes();
            return minutes >= timeout;
        } catch (IOException ex) {
            return true;
        }
    }

    private static final String TIME_STAMP_FORMAT = "yyyyMMddHHmmss";
    private static final String MODIFIED = "modified";
}
