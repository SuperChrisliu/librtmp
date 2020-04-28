package com.tsingtech.librtmp.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static io.netty.buffer.Unpooled.buffer;

/**
 * Author: chrisliu
 * Date: 2019/8/24 20:33
 * Mail: gwarmdll@gmail.com
 */
public class SimpleHandshakeHandler extends ByteToMessageDecoder {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ThreadLocalRandom random =  ThreadLocalRandom.current();
        ByteBuf heapBuffer = buffer(1537);
        heapBuffer.writeByte(0x03);
        int time = (int) (System.currentTimeMillis() / 1000);
        heapBuffer.writeByte((byte) (time >>> 24));
        heapBuffer.writeByte((byte) (time >>> 16));
        heapBuffer.writeByte((byte) (time >>> 8));
        heapBuffer.writeByte((byte) time);
        heapBuffer.writeByte(0);
        heapBuffer.writeByte(0);
        heapBuffer.writeByte(0);
        heapBuffer.writeByte(0);
        for (; heapBuffer.writerIndex() < 1537; )
            for (int rnd = random.nextInt(), n = Math.min(heapBuffer.capacity() - heapBuffer.writerIndex(), Integer.SIZE/Byte.SIZE); n-- > 0; rnd >>= Byte.SIZE)
                heapBuffer.writeByte((byte)(0x0f + rnd % (256 - 0x0f * 2))) ;
        ctx.writeAndFlush(heapBuffer);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if(in.readableBytes() < 1 + 1536 * 2) {
            return;
        }
        if (in.readByte() == 0x03) {
            ctx.writeAndFlush(in.slice(1, 1536).retain());
            in.readerIndex(1536 * 2 + 1);
            ctx.channel().pipeline().remove(this);
        } else {
            System.out.println("failed to handshake");
        }
        ctx.fireChannelActive();
    }
}
