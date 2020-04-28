package com.tsingtech.librtmp.handler;

import com.github.faucamp.simplertmp.packets.Command;
import com.tsingtech.librtmp.RtmpClient;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * Author: chrisliu
 * Date: 2019/8/15 10:00
 * Mail: gwarmdll@gmail.com
 */
public class SrsRtmpHandler extends SimpleChannelInboundHandler<com.github.faucamp.simplertmp.packets.RtmpPacket> implements ChannelOutboundHandler {
    RtmpClient rtmpClient;

    public SrsRtmpHandler(RtmpClient rtmpClient) {
        this.rtmpClient = rtmpClient;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ctx.writeAndFlush(com.github.faucamp.simplertmp.packets.CommandFactory.connectCommand());
    }
int chunksize;
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, com.github.faucamp.simplertmp.packets.RtmpPacket rtmpPacket) throws Exception {
        switch (rtmpPacket.getHeader().getMessageType()) {
            case SET_CHUNK_SIZE:
                chunksize = ((com.github.faucamp.simplertmp.packets.SetChunkSize)rtmpPacket).getChunkSize();
                rtmpClient.setRxChunkSize(chunksize);
                ctx.writeAndFlush(new com.github.faucamp.simplertmp.packets.SetChunkSize(60000));
                rtmpClient.setTxChunkSize(60000);
                System.out.println("SetChunkSize");
                break;
            case ABORT:
                rtmpClient.getChunkStreamInfo(((com.github.faucamp.simplertmp.packets.Abort) rtmpPacket).getChunkStreamId()).clearStoredChunks();
                break;
            case USER_CONTROL_MESSAGE:
                com.github.faucamp.simplertmp.packets.UserControl user = (com.github.faucamp.simplertmp.packets.UserControl) rtmpPacket;
                switch (user.getType()) {
                    case STREAM_BEGIN:
                        System.out.println("handleRxPacketLoop(): Receive STREAM_BEGIN");
                        break;
                    case PING_REQUEST:
                        com.github.faucamp.simplertmp.io.ChunkStreamInfo channelInfo = rtmpClient.getChunkStreamInfo(com.github.faucamp.simplertmp.io.ChunkStreamInfo.RTMP_CID_PROTOCOL_CONTROL);
//                        Log.d(TAG, "handleRxPacketLoop(): Sending PONG reply..");
                        com.github.faucamp.simplertmp.packets.UserControl pong = new com.github.faucamp.simplertmp.packets.UserControl(user, channelInfo);
//                        sendRtmpPacket(pong);
                        break;
                    case STREAM_EOF:
                        System.out.println("handleRxPacketLoop(): Stream EOF reached, closing RTMP writer...");
                        break;
                    default:
                        // Ignore...
                        break;
                }
                break;
            case WINDOW_ACKNOWLEDGEMENT_SIZE:
                com.github.faucamp.simplertmp.packets.WindowAckSize windowAckSize = (com.github.faucamp.simplertmp.packets.WindowAckSize) rtmpPacket;
                int size = windowAckSize.getAcknowledgementWindowSize();
                break;
            case SET_PEER_BANDWIDTH:
//                SetPeerBandwidth bw = (SetPeerBandwidth) rtmpPacket;
//                rtmpSessionInfo.setAcknowledgmentWindowSize(bw.getAcknowledgementWindowSize());
//                int acknowledgementWindowsize = rtmpSessionInfo.getAcknowledgementWindowSize();
//                ChunkStreamInfo chunkStreamInfo = rtmpSessionInfo.getChunkStreamInfo(ChunkStreamInfo.RTMP_CID_PROTOCOL_CONTROL);
//                Log.d(TAG, "handleRxPacketLoop(): Send acknowledgement window size: " + acknowledgementWindowsize);
//                ctx.writeAndFlush(new WindowAckSize(acknowledgementWindowsize, chunkStreamInfo));
//                socket.setSendBufferSize(acknowledgementWindowsize);
                break;
            case COMMAND_AMF0:
                handleRxInvoke((Command) rtmpPacket, ctx);
                break;
            default:
                System.out.println("handleRxPacketLoop(): Not handling unimplemented/unknown packet of type: " + rtmpPacket.getHeader().getMessageType());
                break;
        }
    }

    private void handleRxInvoke(Command invoke, ChannelHandlerContext ctx) throws IOException {
        String commandName = invoke.getCommandName();

        if (commandName.equals("_result")) {
            // This is the result of one of the methods invoked by us
            String method = rtmpClient.takeInvokedCommand(invoke.getTransactionId());

            System.out.println("handleRxInvoke: Got result for invoked method: " + method);
            if ("connect".equals(method)) {
                System.out.println("createStream(): Sending createStream command...");
                ctx.writeAndFlush(com.github.faucamp.simplertmp.packets.CommandFactory.createStream());
            } else if ("createStream".contains(method)) {
                ctx.writeAndFlush(com.github.faucamp.simplertmp.packets.CommandFactory.publish((int) ((com.github.faucamp.simplertmp.amf.AmfNumber) invoke.getData().get(1)).getValue(), rtmpClient.getStreamName()));
            } else if ("releaseStream".contains(method)) {
                System.out.println("handleRxInvoke(): 'releaseStream'");
            } else if ("FCPublish".contains(method)) {
                System.out.println("handleRxInvoke(): 'FCPublish'");
            } else {
                System.out.println("handleRxInvoke(): '_result' message received for unknown method: " + method);
            }
        }
//        } else if (commandName.equals("onBWDone")) {
//            Log.d(TAG, "handleRxInvoke(): 'onBWDone'");
//        } else if (commandName.equals("onFCPublish")) {
//            Log.d(TAG, "handleRxInvoke(): 'onFCPublish'");
//        } else
        if (commandName.equals("onStatus")) {
            String code = ((com.github.faucamp.simplertmp.amf.AmfString) ((com.github.faucamp.simplertmp.amf.AmfObject) invoke.getData().get(1)).getProperty("code")).getValue();
            System.out.println("handleRxInvoke(): onStatus " + code);
            if (code.equals("NetStream.Publish.Start")) {
                rtmpClient.setInited();
//                data = readAllBytes(get("C:/Users/89438/Desktop/720p.h264.raw"));
//                SrsFlvMuxer srsFlvMuxer = new SrsFlvMuxer(ctx, currentStreamId);
//                for (;;) {
//                    searchNextFrame(true);
//                    int type = data[currentFrameIndex] & 0x1f;
//
//                    if (type == 7) {
//                        sendppssps = true;
//                        sps = currentFrameIndex;
//                        spsLength = currentFrameLength;
//                    } else if (type == 8) {
//                        sendppssps = true;
//                        pps = currentFrameIndex;
//                        ppsLength = currentFrameLength;
//                    } else if (type == 5) {
//                        if (sendppssps) {
//                            byte[] spsData = new byte[spsLength];
//                            System.arraycopy(data, sps , spsData, 0, spsLength);
//                            byte[] ppsData = new byte[ppsLength];
//                            System.arraycopy(data, pps , ppsData, 0, ppsLength);
//                            srsFlvMuxer.setPPSSPS(ByteBuffer.wrap(spsData), ByteBuffer.wrap(ppsData));
//                            try {
//                                Thread.sleep(40);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                            sendppssps = false;
//                        }
//                        byte[] temp = new byte[currentFrameLength];
//                        System.arraycopy(data, currentFrameIndex , temp, 0, currentFrameLength);
//                        srsFlvMuxer.writeSampleData(ByteBuffer.wrap(temp), SrsFlvMuxer.SrsCodecVideoAVCFrame.KeyFrame);
//                        try {
//                            Thread.sleep(40);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    } else if (type == 1){
//                        byte[] temp = new byte[currentFrameLength];
//                        System.arraycopy(data, currentFrameIndex , temp, 0, currentFrameLength);
//                        srsFlvMuxer.writeSampleData(ByteBuffer.wrap(temp), SrsFlvMuxer.SrsCodecVideoAVCFrame.InterFrame);
//                        try {
//                            Thread.sleep(40);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }


            }
        }
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.bind(localAddress, promise);
    }


    @Override
    public void connect(ChannelHandlerContext channelHandlerContext, SocketAddress socketAddress, SocketAddress socketAddress1, ChannelPromise channelPromise) throws Exception {
        channelHandlerContext.connect(socketAddress, socketAddress1, channelPromise);
//        writeCommandExpectingResult(channelHandlerContext.channel(), Command.connect(options));
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.disconnect(promise);
    }


    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.close(promise);
    }


    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.deregister(promise);
    }

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ctx.write(msg, promise);
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

}
