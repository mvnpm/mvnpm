package io.mvnpm.npm.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * To handle some Repository values that comes in with a String only
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public class RepositoryDeserializer extends StdDeserializer<Repository> {

    public RepositoryDeserializer() {
        this(null);
    }

    public RepositoryDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Repository deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JacksonException {
        try {
            return jp.readValueAs(Repository.class);
        } catch (Throwable t) {
            // Let try with String
            try {
                String url = jp.readValueAs(String.class);
                return new Repository(null, url, null);
            } catch (Throwable tt) {
                tt.printStackTrace();
                return null;
            }
        }
    }

}
