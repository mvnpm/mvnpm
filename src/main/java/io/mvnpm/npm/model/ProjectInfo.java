package io.mvnpm.npm.model;

import java.util.Set;

/**
 * Lightweight projection of a Project for caching.
 * Only retains the fields callers actually need, dropping
 * the large per-version time map and unused metadata fields.
 */
public record ProjectInfo(
        DistTags distTags,
        Set<String> versions,
        String lastModified) {

    public static ProjectInfo from(Project project) {
        String modified = null;
        if (project.time() != null) {
            modified = project.time().get("modified");
        }
        return new ProjectInfo(project.distTags(), project.versions(), modified);
    }
}