package io.mvnpm.log;

import static io.mvnpm.mavencentral.sync.Stage.CLOSED;
import static io.mvnpm.mavencentral.sync.Stage.INIT;
import static io.mvnpm.mavencentral.sync.Stage.RELEASED;
import static io.mvnpm.mavencentral.sync.Stage.RELEASING;
import static io.mvnpm.mavencentral.sync.Stage.UPLOADED;
import static io.mvnpm.mavencentral.sync.Stage.UPLOADING;

import io.mvnpm.mavencentral.sync.CentralSyncItem;

public class EventLogEntryUtil {

    private EventLogEntryUtil() {
    }

    public static EventLogEntry toEventLogEntry(CentralSyncItem centralSyncItem) {
        return EventLogEntryUtil.toEventLogEntry(centralSyncItem,
                EventLogEntryUtil.generateMessage(centralSyncItem));
    }

    public static EventLogEntry toEventLogEntry(CentralSyncItem centralSyncItem, String message) {
        return EventLogEntryUtil.toEventLogEntry(centralSyncItem, message, "lightgreen");
    }

    public static EventLogEntry toEventLogEntry(CentralSyncItem centralSyncItem, String message, String color) {
        EventLogEntry eventLogEntry = new EventLogEntry();

        eventLogEntry.groupId = centralSyncItem.groupId;
        eventLogEntry.artifactId = centralSyncItem.artifactId;
        eventLogEntry.version = centralSyncItem.version;
        eventLogEntry.stage = centralSyncItem.stage;
        eventLogEntry.message = message;
        eventLogEntry.time = centralSyncItem.stageChangeTime;
        eventLogEntry.color = color;
        return eventLogEntry;
    }

    private static String generateMessage(CentralSyncItem centralSyncItem) {
        return switch (centralSyncItem.stage) {
            case INIT -> "Syncing initialized";
            case UPLOADING -> "Uploading to OSS sonatype (" + centralSyncItem.uploadAttempts + ")";
            case UPLOADED -> "Uploaded to OSS sonatype, now validating (" + centralSyncItem.promotionAttempts + ")";
            case CLOSED -> "Closed and validated. Will be auto releasing soon (" + centralSyncItem.promotionAttempts + ")";
            case RELEASING -> "Closed, now releasing to Maven central (" + centralSyncItem.promotionAttempts + ")";
            case RELEASED -> "Released to Maven central";
            case ERROR -> "Error in workflow after " + centralSyncItem.uploadAttempts + " upload and "
                    + centralSyncItem.promotionAttempts + " promotion attempts";
            default -> centralSyncItem.stage.name().toLowerCase();
        };

    }
}
