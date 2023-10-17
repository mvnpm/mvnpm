package io.mvnpm.npm.model;

import java.net.URL;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public record Project(
        @JsonProperty("name") Name name,
        String description,
        @JsonProperty("dist-tags") DistTags distTags,
        URL homepage,
        @JsonDeserialize(using = LicenseDeserializer.class) License license,
        @JsonDeserialize(using = VersionDeserializer.class) Set<String> versions,
        Map<String, String> time) {
}
