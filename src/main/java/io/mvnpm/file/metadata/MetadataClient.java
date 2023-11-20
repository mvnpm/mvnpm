package io.mvnpm.file.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.mvnpm.Constants;
import io.mvnpm.composite.CompositeService;
import io.mvnpm.file.FileStore;
import io.mvnpm.file.FileUtil;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.Project;
import io.mvnpm.version.InvalidVersionException;
import io.mvnpm.version.Version;
import io.quarkus.logging.Log;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a maven-metadata.xml from the NPM Project
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class MetadataClient {
    private final MetadataXpp3Writer metadataXpp3Writer = new MetadataXpp3Writer();

    @Inject
    NpmRegistryFacade npmRegistryFacade;

    @Inject
    CompositeService compositeService;

    @Inject
    FileStore fileStore;

    @ConfigProperty(name = "mvnpm.metadata-timeout.minutes")
    int timeout;

    public StreamingOutput getMetadataXml(Name name) {
        Path localFilePath = fileStore.getLocalMetadataXmlFullPath(name);
        // Create if if does not exist.
        if (!Files.exists(localFilePath) || !Files.isRegularFile(localFilePath) || isOlderThanTimeout(localFilePath)) {
            createDir(localFilePath);
            try (OutputStream os = Files.newOutputStream(localFilePath)) {
                Metadata metadata = getMetadata(name);
                metadataXpp3Writer.write(os, metadata);
                FileUtil.createSha1(localFilePath);
                FileUtil.createMd5(localFilePath);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        return toStreamingOutputStream(localFilePath);
    }

    public StreamingOutput getMetadataSha1(Name name) {

        Path localSha1Path = fileStore.getLocalMetadataXmlSha1FullPath(name);

        // Create if if does not exist.
        if (!Files.exists(localSha1Path) || !Files.isRegularFile(localSha1Path)) {
            createDir(localSha1Path);
            Path localFilePath = fileStore.getLocalMetadataXmlFullPath(name);
            FileUtil.createSha1(localFilePath);
        }
        return toStreamingOutputStream(localSha1Path);
    }

    public StreamingOutput getMetadataMd5(Name name) {

        Path localMd5Path = fileStore.getLocalMetadataXmlMd5FullPath(name);

        // Create if if does not exist.
        if (!Files.exists(localMd5Path) || !Files.isRegularFile(localMd5Path)) {
            Path localFilePath = fileStore.getLocalMetadataXmlFullPath(name);
            FileUtil.createMd5(localFilePath);
        }
        return toStreamingOutputStream(localMd5Path);
    }

    private Metadata getMetadata(Name name) {
        Metadata metadata = new Metadata();
        metadata.setGroupId(name.mvnGroupId);
        metadata.setArtifactId(name.mvnArtifactId);
        metadata.setVersioning(getVersioning(name));
        return metadata;

    }

    public Versioning getVersioning(Name name) {
        if (name.isInternal()) {
            return getInternalVersioning(name);
        } else {
            return getNpmVersioning(name);
        }
    }

    private Versioning getInternalVersioning(Name name) {
        Versioning versioning = new Versioning();

        Map<String, Date> versions = compositeService.getVersions(name);
        if (versions.isEmpty())
            throw new RuntimeException("No version found for " + name.displayName);

        boolean isFirst = true;
        for (Map.Entry<String, Date> version : versions.entrySet()) {
            if (isFirst) {
                isFirst = false;
                versioning.setLatest(version.getKey());
                versioning.setRelease(version.getKey());
                versioning.setLastUpdatedTimestamp(version.getValue());
            }

            try {
                Version v = Version.fromString(version.getKey());
                // Ignore pre release
                if (v.qualifier() == null) {
                    versioning.addVersion(v.toString());
                }
            } catch (InvalidVersionException ive) {
                Log.warn("Ignoring version [" + ive.getVersion() + "] for " + name.displayName);
            }
        }

        return versioning;
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

    private void createDir(Path localFilePath){
        Path parentDir = localFilePath.getParent();
        if (!Files.exists(parentDir)){
            try {
                Files.createDirectories(parentDir);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }
    
    private StreamingOutput toStreamingOutputStream(Path localFilePath) {
        return outputStream -> {
            try (InputStream fileInputStream = Files.newInputStream(localFilePath)) {
                int bytesRead;
                byte[] buffer = new byte[4096];
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Error streaming file content", e);
            }
        };
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
