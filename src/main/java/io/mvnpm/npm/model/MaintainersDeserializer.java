package io.mvnpm.npm.model;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * To handle some Maintainers values that comes in with a String only
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public class MaintainersDeserializer extends StdDeserializer<List<Maintainer>> {

    public MaintainersDeserializer() {
        this(null);
    }

    public MaintainersDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public List<Maintainer> deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JacksonException {
        try {
            return jp.readValueAs(new TypeReference<List<Maintainer>>() {
            });
        } catch (Throwable t) {
            try {
                String email = jp.readValueAs(String.class);
                return List.of(new Maintainer(null, null, email));
            } catch (Throwable ttt) {
                try {
                    String text = jp.getText();
                    return List.of(new Maintainer(null, null, text));
                } catch (Throwable tttt) {
                    ttt.printStackTrace();
                    return List.of();
                }
            }
        }
    }
}
