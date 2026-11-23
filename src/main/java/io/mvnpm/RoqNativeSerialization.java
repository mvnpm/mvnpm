package io.mvnpm;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Roq 2.2.1 Java-serializes recorded pages to {@code roq-recorded/*.ser} resources, but only registers its own
 * records for native serialization. The JDK types inside them must be registered too, otherwise the native
 * application fails at startup with an {@code InvalidClassException} (serialVersionUID mismatch).
 * <p>
 * Workaround until Roq registers them itself.
 */
@RegisterForReflection(classNames = {
        "java.util.ArrayList",
        "java.util.HashMap",
        "java.util.LinkedHashMap",
        "java.util.CollSer",
        "java.lang.Boolean",
        "java.lang.Integer",
        "java.lang.Number"
}, serialization = true)
public final class RoqNativeSerialization {

    private RoqNativeSerialization() {
    }
}
