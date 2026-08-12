package io.mvnpm.maven.sync;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Encoding the websocket message
 *
 * @author Phillip Kruger(phillip.kruger@gmail.com)
 */
public class SyncItemEncoder implements Encoder.Text<SyncItem>, Decoder.Text<SyncItem> {

    private final ObjectMapper objectMapper;

    public SyncItemEncoder() {
        this.objectMapper = CDI.current().select(ObjectMapper.class).get();
    }

    @Override
    public String encode(SyncItem object) throws EncodeException {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            throw new EncodeException(object, "Error encoding SyncItem to Json", ex);
        }
    }

    @Override
    public SyncItem decode(String s) throws DecodeException {
        try {
            SyncItem o = objectMapper.readValue(s, SyncItem.class);
            return o;
        } catch (JsonProcessingException ex) {
            throw new DecodeException(s, "Error decoding json to SyncItem", ex);
        }
    }

    @Override
    public boolean willDecode(String s) {
        return true;
    }

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
}
