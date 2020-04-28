package com.tsingtech.librtmp.handler;

import com.tsingtech.librtmp.RtmpClient;
import com.tsingtech.librtmp.codec.SrsRtmpDecoder;
import com.tsingtech.librtmp.codec.SrsRtmpEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

/**
 * Author: chrisliu
 * Date: 2019/8/24 21:00
 * Mail: gwarmdll@gmail.com
 */
public class RtmpClientInitializer extends ChannelInitializer {

    RtmpClient rtmpClient;

    public RtmpClientInitializer(RtmpClient rtmpClient) {
        this.rtmpClient = rtmpClient;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ch.pipeline().addLast(new SimpleHandshakeHandler())
                .addLast(new SrsRtmpEncoder(this.rtmpClient))
                .addLast(new SrsRtmpDecoder(this.rtmpClient))
                .addLast(new SrsRtmpHandler(this.rtmpClient));
    }
}