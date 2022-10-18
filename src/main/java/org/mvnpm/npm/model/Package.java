package org.mvnpm.npm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URL;
import java.util.List;
import java.util.Map;

public record Package(
        @JsonProperty("_id")
        String id,
        FullName name,
        String version, 
        String description,
        String license,
        Author author,
        URL homepage,
        Repository repository,
        Bugs bugs,
        String main,
        String module,
        String type,
        List<Maintainer> maintainers,
        Map<FullName,String> dependencies,
        Dist dist) {
}
