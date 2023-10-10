package io.mvnpm.npm.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SearchResult(
        @JsonProperty("package") SearchItem item,
        Map<String, String> flags) {
}
