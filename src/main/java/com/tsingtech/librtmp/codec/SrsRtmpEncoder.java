package com.tsingtech.librtmp.codec;

import com.github.faucamp.simplertmp.io.ChunkStreamInfo;
import com.github.faucamp.simplertmp.packets.Audio;
import com.github.faucamp.simplertmp.packets.Command;
import com.github.faucamp.simplertmp.packets.RtmpPacket;
import com.github.faucamp.simplertmp.packets.Video;
import com.tsingtech.librtmp.RtmpClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Author: chrisliu
 * Date: 2019/8/15 9:35
 * Mail: gwarmdll@gmail.com
 */
public class SrsRtmpEncoder extends ChannelDuplexHandler {
    static final Pattern rtmpUrlPattern = Pattern.compile("^rtmp://([^/:]+)(:(\\d+))*/([^/]+)(/(.*))*$");
    RtmpClient rtmpClient;
    public SrsRtmpEncoder(RtmpClient rtmpClient) {
        this.rtmpClient = rtmpClient;
    }
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ctx.writeAndFlush(encode((RtmpPacket)msg), promise);
    }


    public ByteBuf encode(final RtmpPacket rtmpPacket) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChunkStreamInfo chunkStreamInfo = rtmpClient.getChunkStreamInfo(rtmpPacket.getHeader().getChunkStreamId());
        chunkStreamInfo.setPrevHeaderTx(rtmpPacket.getHeader());
        if (!(rtmpPacket instanceof Video || rtmpPacket instanceof Audio)) {
//                rtmpPacket.getHeader().setAbsoluteTimestamp((int) chunkStreamInfo.markAbsoluteTimestampTx());
        }
        try {
            rtmpPacket.writeTo(baos, rtmpClient.getTxChunkSize(), chunkStreamInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        System.out.println("wrote packet: " + rtmpPacket + ", size: " + rtmpPacket.getHeader().getPacketLength());
        if (rtmpPacket instanceof Command) {
            rtmpClient.addInvokedCommand(((Command) rtmpPacket).getTransactionId(), ((Command) rtmpPacket).getCommandName());
        }
//        final ByteBuf out = Unpooled.buffer(
//                RtmpHeader.MAX_ENCODED_SIZE + header.getSize() + header.getSize() / chunkSize);
        return Unpooled.wrappedBuffer(baos.toByteArray());
    }
}
