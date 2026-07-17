package io.mvnpm.maven.api;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;

/**
 * An interface for maven-clients.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
public interface MavenClient {

    /**
     * Multipart form for maven-central upload.
     */
    static class BundleUploadForm {

        @FormParam("bundle")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public byte[] bundle;
    }

    /**
     * Publishing enumeration.
     */
    static enum PublishingType {
        AUTOMATIC,
        USER_MANAGED
    }
}
