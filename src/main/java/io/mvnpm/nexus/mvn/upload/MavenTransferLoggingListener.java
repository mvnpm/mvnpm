package io.mvnpm.nexus.mvn.upload;

import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;

import io.quarkus.logging.Log;

public final class MavenTransferLoggingListener extends AbstractTransferListener {

    @Override
    public void transferInitiated(TransferEvent event) {
        final TransferResource resource = event.getResource();

        switch (event.getRequestType()) {
            case PUT -> Log.infof(
                    "Uploading %s to %s",
                    fileName(resource),
                    resource.getRepositoryId());
            case GET -> Log.debugf(
                    "Downloading %s from %s",
                    fileName(resource),
                    resource.getRepositoryId());
            case GET_EXISTENCE -> Log.debugf(
                    "Checking %s in %s",
                    fileName(resource),
                    resource.getRepositoryId());
        }

        Log.tracef(
                "Maven transfer URL: %s%s",
                resource.getRepositoryUrl(),
                resource.getResourceName());
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        final TransferResource resource = event.getResource();

        switch (event.getRequestType()) {
            case PUT -> Log.infof(
                    "Uploaded %s to %s (%s)",
                    fileName(resource),
                    resource.getRepositoryId(),
                    formatBytes(event.getTransferredBytes()));

            case GET -> Log.debugf(
                    "Downloaded %s from %s (%s)",
                    fileName(resource),
                    resource.getRepositoryId(),
                    formatBytes(event.getTransferredBytes()));

            case GET_EXISTENCE -> {
                // Nothing useful to log at normal level.
            }
        }
    }

    @Override
    public void transferFailed(TransferEvent event) {
        final TransferResource resource = event.getResource();

        if (event.getRequestType() == TransferEvent.RequestType.PUT) {
            Log.errorf(
                    event.getException(),
                    "Failed to upload %s to %s",
                    fileName(resource),
                    resource.getRepositoryId());
        } else {
            /*
             * Failed GETs are not necessarily errors.
             *
             * For example, Maven Resolver can look for maven-metadata.xml
             * which doesn't exist yet during the first deployment.
             */
            Log.debugf(
                    event.getException(),
                    "Could not download %s from %s",
                    fileName(resource),
                    resource.getRepositoryId());
        }
    }

    @Override
    public void transferCorrupted(TransferEvent event) {
        final TransferResource resource = event.getResource();

        Log.warnf(
                event.getException(),
                "Transfer of %s from %s was corrupted",
                fileName(resource),
                resource.getRepositoryId());
    }

    private static String fileName(TransferResource resource) {
        final String name = resource.getResourceName();
        final int slash = name.lastIndexOf('/');
        return slash < 0 ? name : name.substring(slash + 1);
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1_000) {
            return bytes + " B";
        }
        if (bytes < 1_000_000) {
            return "%.1f kB".formatted(bytes / 1_000.0);
        }
        if (bytes < 1_000_000_000) {
            return "%.1f MB".formatted(bytes / 1_000_000.0);
        }
        return "%.1f GB".formatted(bytes / 1_000_000_000.0);
    }
}
