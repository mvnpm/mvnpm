package io.mvnpm.log;

import static io.mvnpm.mavencentral.sync.Stage.CLOSED;
import static io.mvnpm.mavencentral.sync.Stage.INIT;
import static io.mvnpm.mavencentral.sync.Stage.RELEASED;
import static io.mvnpm.mavencentral.sync.Stage.RELEASING;
import static io.mvnpm.mavencentral.sync.Stage.UPLOADED;
import static io.mvnpm.mavencentral.sync.Stage.UPLOADING;

import io.mvnpm.mavencentral.sync.CentralSyncItem;
import io.mvnpm.mavencentral.sync.Stage;

public class EventLogEntryUtil {

    private EventLogEntryUtil() {
    }

    public static EventLogEntry toEventLogEntry(CentralSyncItem centralSyncItem) {
        return EventLogEntryUtil.toEventLogEntry(centralSyncItem,
                EventLogEntryUtil.generateMessage(centralSyncItem.stage));
    }

    public static EventLogEntry toEventLogEntry(CentralSyncItem centralSyncItem, String message) {
        return EventLogEntryUtil.toEventLogEntry(centralSyncItem, message, "lightgreen");
    }

    public static EventLogEntry toEventLogEntry(CentralSyncItem centralSyncItem, String message, String color) {
        EventLogEntry eventLogEntry = new EventLogEntry();

        eventLogEntry.groupId = centralSyncItem.name.mvnGroupId;
        eventLogEntry.artifactId = centralSyncItem.name.mvnArtifactId;
        eventLogEntry.version = centralSyncItem.version;
        eventLogEntry.stage = centralSyncItem.stage;
        eventLogEntry.message = message;
        eventLogEntry.time = centralSyncItem.stageChangeTime;
        eventLogEntry.color = color;
        return eventLogEntry;
    }

    private static String generateMessage(Stage stage) {
        return switch (stage) {
            case INIT -> "Syncing initialized";
            case UPLOADING -> "Uploading to OSS sonatype";
            case UPLOADED -> "Uploaded to OSS sonatype, now validating";
            case CLOSED -> "Closed and validated. Will be auto releasing soon";
            case RELEASING -> "Closed, now releasing to Maven central";
            case RELEASED -> "Released to Maven central";
            default -> stage.name().toLowerCase();
        };

    }
}
