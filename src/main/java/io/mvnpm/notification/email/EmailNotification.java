package io.mvnpm.notification.email;

import io.mvnpm.mavencentral.sync.CentralSyncItem;
import io.mvnpm.notification.Notification;
import io.mvnpm.notification.NotificationFormatter;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Send an email on new release
 * @author Phillip Kruger (phillip.kruger@gmail.com
 */
@ApplicationScoped
public class EmailNotification {
    
    @Inject
    Mailer mailer;
    
    @ConsumeEvent("artifact-released-to-central")
    @Blocking
    public void artifactReleased(CentralSyncItem centralSyncItem) {
        Notification notification = NotificationFormatter.getNotificationAsHTML(centralSyncItem); 
        mailer.send(Mail.withHtml("mvnpm-releases@googlegroups.com", notification.title(), notification.body()));
    }
    
}
