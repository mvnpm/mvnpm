package org.mvnpm.npm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.net.URL;
import java.util.List;
import java.util.Map;

public record Package(
        @JsonProperty("_id")
        String id,
        Name name,
        String version, 
        String description,
        String license,
        @JsonDeserialize(using = AuthorDeserializer.class)
        Author author,
        URL homepage,
        Repository repository,
        @JsonDeserialize(using = BugsDeserializer.class)
        Bugs bugs,
        String main,
        String module,
        String type,
        List<Maintainer> maintainers,
        Map<Name,String> dependencies,
        Dist dist) {
}
