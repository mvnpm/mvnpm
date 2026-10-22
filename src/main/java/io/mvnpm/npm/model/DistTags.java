package io.mvnpm.npm.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public final class DistTags {
    private final Map<String, String> values;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public DistTags(Map<String, String> values) {
        // npm metadata (and older Nexus fixtures) may contain null tag values, treating these as absent rather than failing while parsing the project
        if (values == null || values.isEmpty()) {
            this.values = Map.of();
        } else {
            java.util.Map<String, String> tags = new java.util.HashMap<>();
            values.forEach((tag, version) -> {
                if (tag != null && version != null) {
                    tags.put(tag, version);
                }
            });
            this.values = Map.copyOf(tags);
        }
    }

    public DistTags(String latest, String next) {
        java.util.Map<String, String> tags = new java.util.HashMap<>();
        if (latest != null)
            tags.put("latest", latest);
        if (next != null)
            tags.put("next", next);
        this.values = Map.copyOf(tags);
    }

    public String latest() {
        return values.get("latest");
    }

    public String next() {
        return values.get("next");
    }

    public String get(String tag) {
        return values.get(tag);
    }

    @JsonValue
    public Map<String, String> values() {
        return values;
    }
}
