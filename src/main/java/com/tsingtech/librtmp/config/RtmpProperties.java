package com.tsingtech.librtmp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Author: chrisliu
 * Date: 2019/8/24 17:13
 * Mail: gwarmdll@gmail.com
 */
@Component
@Data
@ConfigurationProperties(RtmpProperties.PREFIX)
public class RtmpProperties {

    public static final String PREFIX = "tsing.rtmp.server";

    String host;
    Integer port;
    String app;
}
