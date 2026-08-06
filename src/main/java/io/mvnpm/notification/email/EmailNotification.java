package io.mvnpm.notification.email;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.mvnpm.maven.api.Stage;
import io.mvnpm.maven.sync.SyncItem;
import io.mvnpm.notification.Notification;
import io.mvnpm.notification.NotificationFormatter;
import io.quarkus.logging.Log;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;

/**
 * Send an email on new release
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com
 */
@ApplicationScoped
public class EmailNotification {

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "mvnpm.email")
    String email;

    @ConsumeEvent("sync-item-stage-change")
    @Blocking
    public void artifactReleased(SyncItem syncItem) {
        if (syncItem.stage.equals(Stage.RELEASED)) {
            Notification notification = new NotificationFormatter(syncItem).getNotificationAsHTML();
            try {
                mailer.send(Mail.withHtml(email, notification.title(), notification.body()));
            } catch (Exception e) {
                if (Log.isDebugEnabled()) {
                    Log.error("Failed to send release notification.", e);
                } else {
                    Log.warn("Failed to send release notification.");
                }
            }

        }
    }
}
