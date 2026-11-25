package io.mvnpm.creator.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.mvnpm.npm.model.NameParser;

class PomServiceTest {

    @Test
    void overrideReplacesVersion() {
        PomService pomService = pomService(Map.of("@hotwired/stimulus", "^3.0.0"));
        assertEquals("[3.0.0,4)", pomService.toVersion(NameParser.fromNpmProject("@hotwired/stimulus"), "^2.4.0"));
    }

    @Test
    void noOverrideKeepsVersion() {
        PomService pomService = pomService(Map.of("lit", "^3.0.0"));
        assertEquals("[2.4.0,3)", pomService.toVersion(NameParser.fromNpmProject("@hotwired/stimulus"), "^2.4.0"));
    }

    private static PomService pomService(Map<String, String> overrides) {
        PomService pomService = new PomService();
        pomService.overridesConfig = () -> overrides;
        return pomService;
    }
}
