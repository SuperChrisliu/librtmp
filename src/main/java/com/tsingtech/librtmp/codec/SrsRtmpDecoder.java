package com.tsingtech.librtmp.codec;

import com.github.faucamp.simplertmp.io.ChunkStreamInfo;
import com.github.faucamp.simplertmp.packets.*;
import com.tsingtech.librtmp.RtmpClient;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.io.IOException;
import java.util.List;

public class SrsRtmpDecoder extends ReplayingDecoder<SrsRtmpDecoder.DecoderState> {

    public enum DecoderState {
        GET_HEADER,
        GET_PAYLOAD
    }
    RtmpClient rtmpClient;
    public SrsRtmpDecoder(RtmpClient rtmpClient) {
        super(DecoderState.GET_HEADER);
        this.rtmpClient = rtmpClient;
    }

    private RtmpHeader header;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        DecoderState state = super.state();
        switch(state) {
            case GET_HEADER:
                header = RtmpHeader.readHeader(in, rtmpClient);
                ChunkStreamInfo chunkStreamInfo = rtmpClient.getChunkStreamInfo(header.getChunkStreamId());
                chunkStreamInfo.setPrevHeaderRx(header);
                if (header.getPacketLength() > rtmpClient.getRxChunkSize()) {
                    System.out.println("header is big!!!!!!");
                }
                checkpoint(DecoderState.GET_PAYLOAD);
            case GET_PAYLOAD:
                if (header.getPacketLength() > in.readableBytes()) {
                    return;
                }
                RtmpPacket rtmpPacket = null;
                switch (header.getMessageType()) {
                    case SET_CHUNK_SIZE:
                        rtmpPacket = new SetChunkSize(header);
                        break;
                    case ABORT:
                        rtmpPacket = new Abort(header);
                        break;
                    case USER_CONTROL_MESSAGE:
                        rtmpPacket = new UserControl(header);
                        break;
                    case WINDOW_ACKNOWLEDGEMENT_SIZE:
                        rtmpPacket = new WindowAckSize(header);
                        break;
                    case SET_PEER_BANDWIDTH:
                        rtmpPacket = new SetPeerBandwidth(header);
                        break;
                    case AUDIO:
                        rtmpPacket = new Audio(header);
                        break;
                    case VIDEO:
                        rtmpPacket = new Video(header);
                        break;
                    case COMMAND_AMF0:
                        rtmpPacket = new Command(header);
                        break;
                    case DATA_AMF0:
                        rtmpPacket = new Data(header);
                        break;
                    case ACKNOWLEDGEMENT:
                        rtmpPacket = new Acknowledgement(header);
                        break;
                    default:
                        throw new IOException("No packet body implementation for message type: " + header.getMessageType());
                }
                rtmpPacket.readBody(in);
                checkpoint(DecoderState.GET_HEADER);
                out.add(rtmpPacket);
                break;
            default:
                throw new RuntimeException("unexpected decoder state: " + state);
        }
    }
}
