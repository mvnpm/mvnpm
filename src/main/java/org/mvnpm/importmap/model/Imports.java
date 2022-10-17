package org.mvnpm.importmap.model;

import java.util.Map;

public record Imports(
        Map<String,String> imports) {
}
