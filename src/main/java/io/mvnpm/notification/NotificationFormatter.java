package io.mvnpm.notification;

import io.mvnpm.mavencentral.sync.CentralSyncItem;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;

/**
 * Format a notification message
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com
 */
public class NotificationFormatter {

    private NotificationFormatter() {
    }

    private static final String MARKDOWN = """
            [mvnpm.org](http://mvnpm.org) has automatically released the following artifact:

            **Group Id:** `%s`
            **Artifact Id:** `%s`
            **Version:** %s

            This represent the NPM Package: `%s`

            Release has been done using the `%s` staging repo
            """;

    private static final String HTML = """
            <a href="http://mvnpm.org">mvnpm.org</a> has automatically released the following artifact: <br/>
            <br/>
            <b>Group Id:</b> <code>%s</code><br/>
            <b>Artifact Id:</b> <code>%s</code><br/>
            <b>Version:</b> %s<br/>
            <br/>
            This represent the NPM Package: <code>%s</code><br/>
            <br/>
            Release has been done using the <code>%s</code> staging repo
            """;

    private static final String ERROR = """
            <a href="http://mvnpm.org">mvnpm.org</a> has failed to released the following artifact: <br/>
            <br/>
            <b>Group Id:</b> <code>%s</code><br/>
            <b>Artifact Id:</b> <code>%s</code><br/>
            <b>Version:</b> %s<br/>
            <br/>
            This represent the NPM Package: <code>%s</code><br/>
            <br/>
            Release has been attempted using the <code>%s</code> staging repo
            """;

    public static Notification getErrorAsHTML(CentralSyncItem centralSyncItem) {
        return getNotificationAsMarkUp(centralSyncItem, ERROR);
    }

    public static Notification getNotificationAsHTML(CentralSyncItem centralSyncItem) {
        return getNotificationAsMarkUp(centralSyncItem, HTML);
    }

    public static Notification getNotificationAsMarkDown(CentralSyncItem centralSyncItem) {
        return getNotificationAsMarkUp(centralSyncItem, MARKDOWN);
    }

    private static Notification getNotificationAsMarkUp(CentralSyncItem centralSyncItem, String format) {
        Name name = NameParser.fromMavenGA(centralSyncItem.groupId, centralSyncItem.artifactId);
        String groupId = name.mvnGroupId;
        String artifactId = name.mvnArtifactId;
        String version = centralSyncItem.version;
        String npmName = name.npmFullName;
        String repo = centralSyncItem.stagingRepoId;

        String title = groupId + ":" + artifactId + ":" + version;
        String body = format.formatted(groupId, artifactId, version, npmName, repo);

        return new Notification(title, body);
    }

}
