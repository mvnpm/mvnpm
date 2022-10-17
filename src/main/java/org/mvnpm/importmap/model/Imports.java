package org.mvnpm.importmap.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Map;

@RegisterForReflection
public record Imports(
        Map<String,String> imports) {
}
