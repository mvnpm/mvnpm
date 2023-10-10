package io.mvnpm.mavencentral.sync;

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
public class CentralSyncItemEncoder implements Encoder.Text<CentralSyncItem>, Decoder.Text<CentralSyncItem> {

    private final ObjectMapper objectMapper;

    public CentralSyncItemEncoder() {
        this.objectMapper = CDI.current().select(ObjectMapper.class).get();
    }

    @Override
    public String encode(CentralSyncItem object) throws EncodeException {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            throw new EncodeException(object, "Error encoding CentralSyncItem to Json", ex);
        }
    }

    @Override
    public CentralSyncItem decode(String s) throws DecodeException {
        try {
            CentralSyncItem o = objectMapper.readValue(s, CentralSyncItem.class);
            return o;
        } catch (JsonProcessingException ex) {
            throw new DecodeException(s, "Error decoding json to CentralSyncItem", ex);
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
