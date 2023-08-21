package io.mvnpm.npm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record SearchResult(
        @JsonProperty("package")
        SearchItem item,
        Map<String,String> flags) {
}
