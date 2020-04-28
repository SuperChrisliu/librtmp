/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.faucamp.simplertmp.amf;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author leoma
 */
public class AmfUndefined implements AmfData {

    @Override
    public void writeTo(OutputStream out) throws IOException {
        out.write(AmfType.UNDEFINED.getValue());
    }

    @Override
    public void readFrom(ByteBuf in) throws IOException {

    }

    public static void writeUndefinedTo(OutputStream out) throws IOException {
        out.write(AmfType.UNDEFINED.getValue());
    }

    @Override
    public int getSize() {
        return 1;
    }    
}
