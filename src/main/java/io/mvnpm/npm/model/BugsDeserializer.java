package io.mvnpm.npm.model;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.net.URL;

/**
 * To handle some Bugs values that comes in with a String only
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public class BugsDeserializer extends StdDeserializer<Bugs> {

    public BugsDeserializer() { 
        this(null); 
    } 

    public BugsDeserializer(Class<?> vc) { 
        super(vc); 
    }
    
    @Override
    public Bugs deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JacksonException {
        try {
            return jp.readValueAs(Bugs.class);
        }catch (Throwable t){
            // Let try with String
            try {
                String url = jp.readValueAs(String.class);
                URL u = new URL(url);
                return new Bugs(u);
            }catch (Throwable tt){
                tt.printStackTrace();
                return null;
            }
        }
    }

}