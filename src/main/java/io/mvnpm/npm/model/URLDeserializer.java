package io.mvnpm.npm.model;

import java.io.IOException;
import java.net.URL;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * To handle some invalid URL values
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public class URLDeserializer extends StdDeserializer<URL> {

    public URLDeserializer() {
        this(null);
    }

    public URLDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public URL deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JacksonException {
        try {
            return jp.readValueAs(URL.class);
        } catch (Throwable t) {
            return null;
        }
    }

}
