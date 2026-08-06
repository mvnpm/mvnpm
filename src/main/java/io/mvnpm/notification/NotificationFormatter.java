package io.mvnpm.notification;

import io.mvnpm.maven.sync.SyncItem;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;

/**
 * Format a notification message
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com
 */
public class NotificationFormatter {

    private static final String MARKDOWN_MSG = """
            [%s](%s) has automatically released the following artifact:

            **Group Id:** `%s`
            **Artifact Id:** `%s`
            **Version:** %s

            This represent the NPM Package: `%s`

            Release has been done using the `%s` staging repo
            """;

    private static final String HTML_MSG = """
            <a href="%s">%s</a> has automatically released the following artifact: <br/>
            <br/>
            <b>Group Id:</b> <code>%s</code><br/>
            <b>Artifact Id:</b> <code>%s</code><br/>
            <b>Version:</b> %s<br/>
            <br/>
            This represent the NPM Package: <code>%s</code><br/>
            <br/>
            Release has been done using the <code>%s</code> staging repo
            """;

    private static final String ERROR_MSG = """
            <a href="%s">%s</a> has failed to released the following artifact: <br/>
            <br/>
            <b>Group Id:</b> <code>%s</code><br/>
            <b>Artifact Id:</b> <code>%s</code><br/>
            <b>Version:</b> %s<br/>
            <br/>
            This represent the NPM Package: <code>%s</code><br/>
            <br/>
            Release has been attempted using the <code>%s</code> staging repo
            """;

    private static enum Format {
        MARKDOWN,
        HTML,
        ERROR;
    }

    @ConfigProperty(name = "mvnpm.domain")
    String domain;

    @ConfigProperty(name = "mvnpm.website")
    String website;

    private final SyncItem syncItem;

    public NotificationFormatter(final SyncItem syncItem) {
        this.syncItem = syncItem;
    }

    public final Notification getErrorAsHTML() {
        return getNotificationAsMarkUp(syncItem, Format.ERROR);
    }

    public final Notification getNotificationAsHTML() {
        return getNotificationAsMarkUp(syncItem, Format.HTML);
    }

    public final Notification getNotificationAsMarkDown() {
        return getNotificationAsMarkUp(syncItem, Format.MARKDOWN);
    }

    private final Notification getNotificationAsMarkUp(SyncItem syncItem, Format format) {
        Name name = NameParser.fromMavenGA(syncItem.groupId, syncItem.artifactId);
        String groupId = name.mvnGroupId;
        String artifactId = name.mvnArtifactId;
        String version = syncItem.version;
        String npmName = name.npmFullName;
        String repo = syncItem.releaseId;

        String title = groupId + ":" + artifactId + ":" + version;

        String body = switch (format) {
            case ERROR:
                yield ERROR_MSG.formatted(website, domain, groupId, artifactId, version, npmName, repo);
            case HTML:
                yield HTML_MSG.formatted(website, domain, groupId, artifactId, version, npmName, repo);
            case MARKDOWN:
                yield MARKDOWN_MSG.formatted(domain, website, groupId, artifactId, version, npmName, repo);
        };

        return new Notification(title, body);
    }

}
