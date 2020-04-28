package com.tsingtech.librtmp.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * Author: chrisliu
 * Date: 2019/6/26 14:46
 * Mail: gwarmdll@gmail.com
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class VideoPacket extends DataPacket {
    private int LIFI;
    private int LFI;

}
