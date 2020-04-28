package com.github.faucamp.simplertmp.amf;

import io.netty.buffer.ByteBuf;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AMF object
 * 
 * @author francois
 */
public class AmfObject implements AmfData {

    protected Map<String, AmfData> properties = new LinkedHashMap<String, AmfData>();
    protected int size = -1;
    /** Byte sequence that marks the end of an AMF object */
    protected static final byte[] OBJECT_END_MARKER = new byte[]{0x00, 0x00, 0x09};

    public AmfObject() {
    }

    public AmfData getProperty(String key) {
        return properties.get(key);
    }

    public void setProperty(String key, AmfData value) {
        properties.put(key, value);
    }

    public void setProperty(String key, boolean value) {
        properties.put(key, new AmfBoolean(value));
    }

    public void setProperty(String key, String value) {
        properties.put(key, new AmfString(value, false));
    }

    public void setProperty(String key, int value) {
        properties.put(key, new AmfNumber(value));
    }

    public void setProperty(String key, double value) {
        properties.put(key, new AmfNumber(value));
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        // Begin the object
        out.write(AmfType.OBJECT.getValue());

        // Write key/value pairs in this object        
        for (Map.Entry<String, AmfData> entry : properties.entrySet()) {
            // The key must be a STRING type, and thus the "type-definition" byte is implied (not included in message)
            AmfString.writeStringTo(out, entry.getKey(), true);
            entry.getValue().writeTo(out);
        }

        // End the object        
        out.write(OBJECT_END_MARKER);

    }

    @Override
    public void readFrom(ByteBuf in) throws IOException {
        int index = in.readerIndex();

        while (true) {
            // Look for the 3-byte object end marker [0x00 0x00 0x09]
            byte[] endMarker = new byte[3];
            in.readBytes(endMarker, 0, endMarker.length);

            if (endMarker[0] == OBJECT_END_MARKER[0] && endMarker[1] == OBJECT_END_MARKER[1] && endMarker[2] == OBJECT_END_MARKER[2]) {
                return;
            } else {
                // End marker not found; reset the stream to the marked position and read an AMF property
                in.readerIndex(index);
//                System.out.println(in.readerIndex());
                // Read the property key...
                String key = AmfString.readStringFrom(in, true);
                // ...and the property value
                AmfData value = AmfDecoder.readFrom(in);
                index = in.readerIndex();
                properties.put(key, value);
            }
        }
    }

    @Override
    public int getSize() {
        if (size == -1) {
            size = 1; // object marker                
            for (Map.Entry<String, AmfData> entry : properties.entrySet()) {
                size += AmfString.sizeOf(entry.getKey(), true);
                size += entry.getValue().getSize();
            }
            size += 3; // end of object marker

        }
        return size;
    }
}
