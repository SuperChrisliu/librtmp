package com.tsingtech.librtmp.vo;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * Author: chrisliu
 * Date: 2019/6/26 14:38
 * Mail: gwarmdll@gmail.com
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class DataPacket {

    protected byte[] simRaw;

    protected double frameRate;

    protected byte PT;
    protected byte[] frameBuffer;

    protected byte logicChannel;

    protected short typeFlag;

    protected short bodyProps;

    protected ByteBuf body;

    protected long timestamp;

    public int getPacketPlace () {
        return typeFlag & 0x0f;
    }

    public String getSim () {
        StringBuilder stringBuilder = new StringBuilder(12);
        for (byte single : simRaw) {
            stringBuilder.append((single >> 4) & 0x0f).append(single & 0x0f);
        }
        return stringBuilder.toString();
    }
}
