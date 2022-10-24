package org.mvnpm.npm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.net.URL;
import java.util.Set;

public record Project (
        @JsonProperty("name")
        Name name, 
        String description,
        @JsonProperty("dist-tags")
        DistTags distTags,
        URL homepage,
        String license,
        @JsonDeserialize(using = VersionDeserializer.class)
        Set<String> versions){
}