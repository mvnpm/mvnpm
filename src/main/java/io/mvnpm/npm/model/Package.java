package io.mvnpm.npm.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.mvnpm.creator.overriding.OverrideDeserializer;
import io.mvnpm.creator.overriding.OverrideStore;

public record Package(
        @JsonProperty("_id") String id,
        Name name,
        String version,
        @JsonDeserialize(using = DescriptionDeserializer.class) String description,
        @JsonDeserialize(using = LicenseDeserializer.class) License license,
        @JsonDeserialize(using = AuthorDeserializer.class) Author author,
        String homepage,
        @JsonDeserialize(using = RepositoryDeserializer.class) Repository repository,
        @JsonDeserialize(using = BugsDeserializer.class) Bugs bugs,
        String main,
        String module,
        String type,
        @JsonDeserialize(using = MaintainersDeserializer.class) List<Maintainer> maintainers,
        Map<Name, String> dependencies,
        Map<Name, String> peerDependencies,
        Map<Name, String> devDependencies,
        Map<Name, String> optionalDependencies,
        Map<String, Map<String, Boolean>> peerDependenciesMeta,
        @JsonDeserialize(using = OverrideDeserializer.class) OverrideStore overrides,
        Dist dist) {

    public boolean isOptionalPeerDependency(Name name) {
        if (peerDependenciesMeta == null) {
            return false;
        }
        Map<String, Boolean> meta = peerDependenciesMeta.get(name.npmFullName);
        return meta != null && Boolean.TRUE.equals(meta.get("optional"));
    }
}
