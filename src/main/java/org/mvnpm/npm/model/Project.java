package org.mvnpm.npm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URL;

public record Project (
        FullName name, 
        String description,
        @JsonProperty("dist-tags")
        DistTags distTags,
        URL homepage,
        String license){
}