package io.mvnpm.npm.model;

import java.io.IOException;
import java.net.URL;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * To handle some License values that comes in with a String only
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public class LicenseDeserializer extends StdDeserializer<License> {

    public LicenseDeserializer() {
        this(null);
    }

    public LicenseDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public License deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JacksonException {
        try {
            return jp.readValueAs(License.class);
        } catch (Throwable t) {
            // Let try with String
            try {
                URL url = jp.readValueAs(URL.class);
                return new License(null, url);
            } catch (Throwable tt) {
                try {
                    String type = jp.readValueAs(String.class);
                    return new License(type, null);
                } catch (Throwable ttt) {
                    try {
                        String type = jp.getText();
                        return new License(type, null);
                    } catch (Throwable tttt) {
                        ttt.printStackTrace();
                        return null;
                    }

                }
            }
        }
    }
}
