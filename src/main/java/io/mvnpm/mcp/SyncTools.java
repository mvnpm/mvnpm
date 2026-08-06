package io.mvnpm.mcp;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import io.mvnpm.log.EventLogEntry;
import io.mvnpm.maven.api.Gav;
import io.mvnpm.maven.api.Stage;
import io.mvnpm.maven.sync.SyncItem;
import io.mvnpm.npm.model.Name;
import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;

public class SyncTools {

    @Inject
    McpNameResolver nameResolver;

    @Tool(description = "Check the Maven repository sync status for a specific NPM package version. Shows current stage, timestamps, and retry counts. Accepts NPM name or Maven coordinates.")
    @Transactional
    ToolResponse check_sync_status(@ToolArg(description = "Package name (NPM or Maven coordinates)") String name,
            @ToolArg(description = "Version (defaults to 'latest')", defaultValue = "latest") String version) {
        Name resolved = nameResolver.resolve(name);
        String ver = nameResolver.resolveVersion(resolved, version);
        SyncItem item = SyncItem.findById(new Gav(resolved.mvnGroupId, resolved.mvnArtifactId, ver));

        StringBuilder sb = new StringBuilder();
        sb.append("Package: ").append(resolved.mvnGroupId).append(":").append(resolved.mvnArtifactId).append(":")
                .append(ver).append("\n");
        if (item == null) {
            sb.append("Status: Not synced (no sync record found)\n");
        } else {
            sb.append("Stage: ").append(item.stage).append("\n");
            if (item.startTime != null)
                sb.append("Started: ").append(item.startTime).append("\n");
            if (item.stageChangeTime != null)
                sb.append("Last Stage Change: ").append(item.stageChangeTime).append("\n");
            if (item.releaseId != null)
                sb.append("Staging Repo ID: ").append(item.releaseId).append("\n");
            sb.append("Creation Attempts: ").append(item.creationAttempts).append("\n");
            sb.append("Upload Attempts: ").append(item.uploadAttempts).append("\n");
            sb.append("Promotion Attempts: ").append(item.promotionAttempts).append("\n");
            sb.append("Dependencies Checked: ").append(item.dependenciesChecked).append("\n");
        }
        return ToolResponse.success(new TextContent(sb.toString()));
    }

    @Tool(description = "List items in the Maven repository sync pipeline. Optionally filter by stage (NONE, PACKAGING, INIT, UPLOADING, UPLOADED, CLOSED, RELEASING, RELEASED, ERROR).")
    @Transactional
    ToolResponse list_sync_pipeline(
            @ToolArg(description = "Filter by stage (optional). Values: NONE, PACKAGING, INIT, UPLOADING, UPLOADED, CLOSED, RELEASING, RELEASED, ERROR") String stage) {
        StringBuilder sb = new StringBuilder();
        if (stage != null && !stage.isBlank()) {
            Stage s = Stage.valueOf(stage.toUpperCase());
            List<SyncItem> items = SyncItem.findByStage(s, 100);
            sb.append("Sync Pipeline - Stage: ").append(s).append(" (").append(items.size()).append(" items):\n\n");
            for (SyncItem item : items) {
                sb.append("  ").append(item.groupId).append(":").append(item.artifactId).append(":")
                        .append(item.version);
                if (item.stageChangeTime != null)
                    sb.append(" (changed: ").append(item.stageChangeTime).append(")");
                sb.append("\n");
            }
        } else {
            sb.append("Sync Pipeline Overview:\n\n");
            for (Stage s : Stage.values()) {
                List<SyncItem> items = SyncItem.findByStage(s, 100);
                if (!items.isEmpty()) {
                    sb.append(s).append(": ").append(items.size()).append(" items\n");
                    for (SyncItem item : items) {
                        sb.append("  ").append(item.groupId).append(":").append(item.artifactId).append(":")
                                .append(item.version).append("\n");
                    }
                    sb.append("\n");
                }
            }
        }
        return ToolResponse.success(new TextContent(sb.toString()));
    }

    @Tool(description = "Get the event log (sync events and errors) for a specific NPM package version. Accepts NPM name or Maven coordinates.")
    @Transactional
    ToolResponse get_event_log(@ToolArg(description = "Package name (NPM or Maven coordinates)") String name,
            @ToolArg(description = "Version (defaults to 'latest')", defaultValue = "latest") String version) {
        Name resolved = nameResolver.resolve(name);
        String ver = nameResolver.resolveVersion(resolved, version);
        List<EventLogEntry> entries = EventLogEntry.findByGav(resolved.mvnGroupId, resolved.mvnArtifactId, ver);

        StringBuilder sb = new StringBuilder();
        sb.append("Event Log: ").append(resolved.mvnGroupId).append(":").append(resolved.mvnArtifactId).append(":")
                .append(ver).append("\n\n");
        if (entries.isEmpty()) {
            sb.append("No events found.\n");
        } else {
            for (EventLogEntry entry : entries) {
                sb.append("[").append(entry.time).append("] ");
                if (entry.stage != null)
                    sb.append(entry.stage).append(" - ");
                sb.append(entry.message).append("\n");
            }
        }
        return ToolResponse.success(new TextContent(sb.toString()));
    }
}
