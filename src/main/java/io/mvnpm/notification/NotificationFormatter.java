package io.mvnpm.notification;

import io.mvnpm.maven.RepoNameVersionType;

/**
 * Format a notification message
 * @author Phillip Kruger (phillip.kruger@gmail.com
 */
public class NotificationFormatter {
    
    private NotificationFormatter(){}
    
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
                                       <b>Group Id:</b> <code>%s</code><br/>
                                       <b>Artifact Id:</b> <code>%s</code><br/>
                                       <b>Version:</b> %s<br/>
                                       <br/>
                                       This represent the NPM Package: <code>%s</code><br/>
                                       <br/>
                                       Release has been done using the <code>%s</code> staging repo
                                       """;
    
    
    public static Notification getNotificationAsHTML(RepoNameVersionType repoNameVersionType){
        return getNotificationAsMarkDown(repoNameVersionType, HTML);
    }
    
    public static Notification getNotificationAsMarkDown(RepoNameVersionType repoNameVersionType){
        return getNotificationAsMarkDown(repoNameVersionType, MARKDOWN);
    }
    
    private static Notification getNotificationAsMarkDown(RepoNameVersionType repoNameVersionType, String format){
        String groupId = repoNameVersionType.nameVersionType().name().mvnGroupId();
        String artifactId = repoNameVersionType.nameVersionType().name().mvnArtifactId();
        String version = repoNameVersionType.nameVersionType().version();
        String npmName = repoNameVersionType.nameVersionType().name().npmFullName();
        String repo = repoNameVersionType.stagingRepoId();

        String title = groupId + ":" + artifactId + ":" + version;
        String body = format.formatted(groupId, artifactId, version, npmName, repo);
        
        return new Notification(title, body);
    }
    
}
