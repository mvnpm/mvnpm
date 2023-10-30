package io.mvnpm.error;

import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.log.EventLogEntry;
import io.mvnpm.mavencentral.sync.Stage;
import io.mvnpm.npm.model.Name;
import io.quarkus.logging.Log;
import io.vertx.mutiny.core.eventbus.EventBus;

/**
 * Error Handling. Since most of the process runs in the back ground, we want to capture error to make them
 * visible when needed
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class ErrorHandlingService {

    @Inject
    EventBus bus;

    public void handle(Name name, String version, Throwable t) {
        handle(name.mvnGroupId(), name.mvnArtifactId(), version, t);
    }

    public void handle(Name name, String version, String message) {
        handle(name.mvnGroupId(), name.mvnArtifactId(), version, message, new RuntimeException(""));
    }

    public void handle(Name name, String version, String message, Throwable t) {
        handle(name.mvnGroupId(), name.mvnArtifactId(), version, message, t);
    }

    public void handle(String groupId, String artifactId, String version, Throwable t) {
        handle(groupId, artifactId, version, null, t);
    }

    public void handle(String groupId, String artifactId, String version) {
        handle(groupId, artifactId, version, null, new RuntimeException(""));
    }

    public void handle(String groupId, String artifactId, String version, String message, Throwable t) {
        EventLogEntry ele = new EventLogEntry();
        ele.time = LocalDateTime.now();
        ele.groupId = groupId;
        ele.artifactId = artifactId;
        ele.version = version;
        ele.stage = Stage.NONE;
        if (message != null) {
            ele.message = message + " - " + t.getMessage();
        } else {
            ele.message = t.getMessage();
        }
        ele.color = "red";
        Log.error(ele.message);
        bus.publish("exception-in-code", ele);
    }

}
