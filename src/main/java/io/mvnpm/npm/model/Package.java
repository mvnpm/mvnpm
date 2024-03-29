package io.mvnpm.npm.model;

import java.net.URL;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public record Package(
        @JsonProperty("_id") String id,
        Name name,
        String version,
        String description,
        @JsonDeserialize(using = LicenseDeserializer.class) License license,
        @JsonDeserialize(using = AuthorDeserializer.class) Author author,
        URL homepage,
        @JsonDeserialize(using = RepositoryDeserializer.class) Repository repository,
        @JsonDeserialize(using = BugsDeserializer.class) Bugs bugs,
        String main,
        String module,
        String type,
        List<Maintainer> maintainers,
        Map<Name, String> dependencies,
        Dist dist) {
}
