package org.mavenpm.npm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URL;

public record Project (
        String name, 
        String description,
        @JsonProperty("dist-tags")
        DistTags distTags,
        URL homepage,
        String license){
}