package io.mvnpm.log;

import static io.mvnpm.maven.api.Stage.CLOSED;
import static io.mvnpm.maven.api.Stage.ERROR;
import static io.mvnpm.maven.api.Stage.INIT;
import static io.mvnpm.maven.api.Stage.RELEASED;
import static io.mvnpm.maven.api.Stage.RELEASING;
import static io.mvnpm.maven.api.Stage.UPLOADED;
import static io.mvnpm.maven.api.Stage.UPLOADING;

import io.mvnpm.maven.sync.SyncItem;

public class EventLogEntryUtil {

    private EventLogEntryUtil() {
    }

    public static EventLogEntry toEventLogEntry(SyncItem syncItem) {
        return EventLogEntryUtil.toEventLogEntry(syncItem, EventLogEntryUtil.generateMessage(syncItem));
    }

    public static EventLogEntry toEventLogEntry(SyncItem syncItem, String message) {
        return EventLogEntryUtil.toEventLogEntry(syncItem, message, "lightgreen");
    }

    public static EventLogEntry toEventLogEntry(SyncItem syncItem, String message, String color) {
        EventLogEntry eventLogEntry = new EventLogEntry();

        eventLogEntry.groupId = syncItem.groupId;
        eventLogEntry.artifactId = syncItem.artifactId;
        eventLogEntry.version = syncItem.version;
        eventLogEntry.stage = syncItem.stage;
        eventLogEntry.message = message;
        eventLogEntry.time = syncItem.stageChangeTime;
        eventLogEntry.color = color;
        return eventLogEntry;
    }

    private static String generateMessage(SyncItem syncItem) {
        return switch (syncItem.stage) {
            case INIT -> "Syncing initialized";
            case UPLOADING -> "Uploading to OSS sonatype (" + syncItem.uploadAttempts + ")";
            case UPLOADED -> "Uploaded to OSS sonatype, now validating (" + syncItem.promotionAttempts + ")";
            case CLOSED -> "Closed and validated. Will be auto releasing soon (" + syncItem.promotionAttempts + ")";
            case RELEASING -> "Closed, now releasing to Maven repository (" + syncItem.promotionAttempts + ")";
            case RELEASED -> "Released to Maven repository";
            case ERROR -> "Error in workflow after " + syncItem.uploadAttempts + " upload and " + syncItem.promotionAttempts
                    + " promotion attempts";
            default -> syncItem.stage.name().toLowerCase();
        };

    }
}
