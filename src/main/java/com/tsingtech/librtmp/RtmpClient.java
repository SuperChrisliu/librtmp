package com.tsingtech.librtmp;

import com.github.faucamp.simplertmp.io.ChunkStreamInfo;
import com.tsingtech.librtmp.config.RtmpProperties;
import com.tsingtech.librtmp.handler.RtmpClientInitializer;
import com.tsingtech.librtmp.handler.SrsFlvMuxer2;
import com.tsingtech.librtmp.util.BeanUtil;
import com.tsingtech.librtmp.vo.DataPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Data;
import lombok.experimental.Accessors;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Author: chrisliu
 * Date: 2019/8/25 11:03
 * Mail: gwarmdll@gmail.com
 * 白茶清欢无别事，我在等风也等你
 */
public final class RtmpClient {

    private ConcurrentLinkedQueue<DataPacket> avQueue;

    public String getStreamName() {
        return streamName;
    }

    private String streamName;

    private String app;

    private String host;

    private int port;

    private String tcUrl;

    private Bootstrap bootstrap;

    /** Default chunk size is 128 bytes */
    private int rxChunkSize = 128;
    private int txChunkSize = 128;
    private Map<Integer, ChunkStreamInfo> chunkChannels = new HashMap<Integer, ChunkStreamInfo>();
    private Map<Integer, String> invokedMethods = new ConcurrentHashMap<Integer, String>();

    private SrsFlvMuxer2 srsFlvMuxer;
    private boolean inited = false;
    private ChannelFuture channelFuture;


    private RtmpClient (Builder builder) {
        bootstrap = new Bootstrap();
        this.avQueue = new ConcurrentLinkedQueue<>();
        this.srsFlvMuxer = new SrsFlvMuxer2();
        ChannelHandlerContext ctx = builder.ctx;
        bootstrap.group(ctx.channel().eventLoop()).channel(NioSocketChannel.class).handler(new RtmpClientInitializer(this));

        try {
            channelFuture = bootstrap.connect(new InetSocketAddress(InetAddress.getByName(builder.host), builder.port));
            channelFuture.addListener(future -> {
                if (future.isSuccess()) {
                    this.inited = true;
                    this.streamName = builder.streamName;
                    RtmpProperties rtmpProperties = BeanUtil.getBean(RtmpProperties.class);
                    this.app = builder.app == null ? rtmpProperties.getApp() : builder.app;

                    if (builder.host == null || builder.port == null) {
                        this.host = rtmpProperties.getHost();
                        this.port = rtmpProperties.getPort();
                    } else {
                        this.host = builder.host;
                        this.port = builder.port;
                    }

                    StringBuilder tcBuilder = new StringBuilder("rtmp://");
                    tcBuilder.append(this.host).append(":").append(this.port).append("/").append(this.app);
                    this.tcUrl = tcBuilder.toString();
                    System.out.println(this.tcUrl);
                } else {
                    System.err.println("Bind attempt failed!");
                    future.cause().printStackTrace();
                    ctx.channel().close();
                }
            });
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void setInited () {
        inited = true;
    }

    public ChunkStreamInfo getChunkStreamInfo(int chunkStreamId) {
        ChunkStreamInfo chunkStreamInfo = chunkChannels.get(chunkStreamId);
        if (chunkStreamInfo == null) {
            chunkStreamInfo = new ChunkStreamInfo();
            chunkChannels.put(chunkStreamId, chunkStreamInfo);
        }
        return chunkStreamInfo;
    }

    public String takeInvokedCommand(int transactionId) {
        return invokedMethods.remove(transactionId);
    }

    public String addInvokedCommand(int transactionId, String commandName) {
        return invokedMethods.put(transactionId, commandName);
    }

    public int getRxChunkSize() {
        return rxChunkSize;
    }

    public void setRxChunkSize(int chunkSize) {
        this.rxChunkSize = chunkSize;
    }

    public int getTxChunkSize() {
        return txChunkSize;
    }

    public void setTxChunkSize(int chunkSize) {
        this.txChunkSize = chunkSize;
    }

    public void publish3 (DataPacket msg) throws Exception {
        this.srsFlvMuxer.write(channelFuture.channel(), msg);
    }

    @Data
    @Accessors(chain = true)
    public static class Builder {
        private String streamName;

        private String app;

        private String host;

        private Integer port;

        private ChannelHandlerContext ctx;

        public static Builder createInstance () {
            return new Builder();
        }

        public RtmpClient build() {
            return new RtmpClient(this);
        }
    }
}
