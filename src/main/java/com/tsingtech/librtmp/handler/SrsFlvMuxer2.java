package com.tsingtech.librtmp.handler;

import com.github.faucamp.simplertmp.packets.Video;
import com.tsingtech.librtmp.vo.DataPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.ReferenceCountUtil;

/**
 * Author: chrisliu
 * Date: 2019/9/5 14:34
 * Mail: gwarmdll@gmail.com
 */
public class SrsFlvMuxer2 {

    private static final long maxOffsetTimestamp = Integer.MAX_VALUE -1;

    private int searchAnnexbFrame(ByteBuf data, int src) {
        int index = src;
        for (; index < data.readableBytes() - 4; index++) {
            // not match.
            if (data.getByte(index) != 0x00 || data.getByte(index + 1) != 0x00) {
                continue;
            }
            // match N[00] 00 00 01, where N>=0
            if (data.getByte(index + 2) == 0x01) {
                return index + 3;
            }
            // match N[00] 00 00 00 01, where N>=0
            if (data.getByte(index + 2) == 0x00 && data.getByte(index + 3) == 0x01) {
                return index + 4;
            }
        }
        return 0;
    }

    public void write(Channel ctx, DataPacket videoPacket) {
        ByteBuf data = videoPacket.getBody();
        int index = 0;
        index = searchAnnexbFrame(data, index);
        try {
            if (index != 0) {
                int offsetTimestamp;
                if (videoPacket.getTimestamp() > Integer.MAX_VALUE) {
                    offsetTimestamp = (int) (videoPacket.getTimestamp() & maxOffsetTimestamp);
                } else {
                    offsetTimestamp = (int) videoPacket.getTimestamp();
                }
//                offsetTimestamp += 1000;
                int type = data.getByte(index) & 0x1f;
                if (type == SrsFlvMuxer.SrsAvcNaluType.SPS) {
                    int spsIndex = index;
                    index = searchAnnexbFrame(data, index);
                    int spsLength = index - spsIndex - 3;
                    int ppsIndex = index;
                    index = searchAnnexbFrame(data, index);
                    int ppsLength = index - ppsIndex - 3;
                    ByteBuf flvTag = muxSPSPPSFlvTag(data.slice(spsIndex, spsLength), data.slice(ppsIndex, ppsLength),
                            offsetTimestamp, offsetTimestamp);

                    ctx.writeAndFlush(createVideoPacket(flvTag, offsetTimestamp));
                    ByteBuf flvTag2 = muxIpbFlvTag(data.slice(index, data.readableBytes() - index), SrsFlvMuxer.SrsCodecVideoAVCFrame.KeyFrame,
                            offsetTimestamp, offsetTimestamp);
                    ctx.writeAndFlush(createVideoPacket(flvTag2, offsetTimestamp));
                } else if (type == SrsFlvMuxer.SrsAvcNaluType.NonIDR) {
                    ByteBuf flvTag = muxIpbFlvTag(data.slice(index, data.readableBytes() - index), SrsFlvMuxer.SrsCodecVideoAVCFrame.InterFrame,
                            offsetTimestamp, offsetTimestamp);
                    ctx.writeAndFlush(createVideoPacket(flvTag, videoPacket.getTimestamp()));
                } else {
                    throw new RuntimeException("unknow type: " + type);
                }
            } else {
                System.out.println("no annexb frame");
            }
        } finally {
            ReferenceCountUtil.release(data);
        }
    }

    private Video createVideoPacket (ByteBuf flvTag, long timestamp) {
        Video video = new Video();
        byte[] data = new byte[flvTag.readableBytes()];
        flvTag.readBytes(data);
        video.setData(data, data.length);
        ReferenceCountUtil.release(flvTag);
        video.getHeader().setAbsoluteTimestamp((int) timestamp);
        video.getHeader().setMessageStreamId(1);
        return video;
    }

    private ByteBuf muxIpbFlvTag (ByteBuf bb, int type, int dts, int pts) {
        ByteBuf fvlTag = Unpooled.directBuffer(9 + bb.readableBytes());
        muxFlvHeader(fvlTag, type, SrsFlvMuxer.SrsCodecVideoAVCType.NALU, dts, pts);
        muxNaluHeader(fvlTag, bb.readableBytes());
        fvlTag.writeBytes(bb);
        return fvlTag;
    }

    private void muxNaluHeader(ByteBuf flvFrame, int NAL_unit_length) {
        // 5.3.4.2.1 Syntax, H.264-AVC-ISO_IEC_14496-15.pdf, page 16
        // lengthSizeMinusOne, or NAL_unit_length, always use 4bytes size

        // mux the avc NALU in "ISO Base Media File Format"
        // from H.264-AVC-ISO_IEC_14496-15.pdf, page 20
        // NALUnitLength
        flvFrame.writeInt(NAL_unit_length);
    }

    private void muxFlvHeader(ByteBuf fvlTag, int frame_type,
                             int avc_packet_type, int dts, int pts) {
        // for h264 in RTMP video payload, there is 5bytes header:
        //      1bytes, FrameType | CodecID
        //      1bytes, AVCPacketType
        //      3bytes, CompositionTime, the cts.

        // @see: E.4.3 Video Tags, video_file_format_spec_v10_1.pdf, page 78
        // Frame Type, Type of video frame.
        // CodecID, Codec Identifier.
        // set the rtmp header
        fvlTag.writeByte(((frame_type << 4) | SrsFlvMuxer.SrsCodecVideo.AVC));

        // AVCPacketType
        fvlTag.writeByte(avc_packet_type);

        // CompositionTime
        // pts = dts + cts, or
        // cts = pts - dts.
        // where cts is the header in rtmp video packet payload header.
        int cts = pts - dts;
        fvlTag.writeByte(cts >> 16);
        fvlTag.writeByte(cts >> 8);
        fvlTag.writeByte(cts);
    }

    private ByteBuf muxSPSPPSFlvTag(ByteBuf sps, ByteBuf pps, int dts, int pts) {
        ByteBuf fvlTag = Unpooled.directBuffer(5 + sps.readableBytes() + pps.readableBytes() + 11);

        muxFlvHeader(fvlTag, SrsFlvMuxer.SrsCodecVideoAVCFrame.KeyFrame, SrsFlvMuxer.SrsCodecVideoAVCType.SequenceHeader, dts, pts);

        // h.264 raw data.
        muxSequenceHeader(fvlTag, sps, pps);
//        System.out.println(String.format("flv: h264 sps/pps sent, sps=%dB, pps=%dB",
//                sps.capacity(), pps.capacity()));
        return fvlTag;
    }

    private void muxSequenceHeader(ByteBuf sequenceHeader, ByteBuf sps, ByteBuf pps) {
        // 5bytes sps/pps header:
        //      configurationVersion, AVCProfileIndication, profile_compatibility,
        //      AVCLevelIndication, lengthSizeMinusOne
        // 3bytes size of sps:
        //      numOfSequenceParameterSets, sequenceParameterSetLength(2B)
        // Nbytes of sps.
        //      sequenceParameterSetNALUnit
        // 3bytes size of pps:
        //      numOfPictureParameterSets, pictureParameterSetLength
        // Nbytes of pps:
        //      pictureParameterSetNALUnit

        // decode the SPS:
        // @see: 7.3.2.1.1, H.264-AVC-ISO_IEC_14496-10-2012.pdf, page 62


        // @see: Annex A Profiles and levels, H.264-AVC-ISO_IEC_14496-10.pdf, page 205
        //      Baseline profile profile_idc is 66(0x42).
        //      Main profile profile_idc is 77(0x4d).
        //      Extended profile profile_idc is 88(0x58).
        byte profile_idc = sps.getByte(1);
        //u_int8_t constraint_set = frame[2];
        byte level_idc = sps.getByte(3);

        // generate the sps/pps header
        // 5.3.4.2.1 Syntax, H.264-AVC-ISO_IEC_14496-15.pdf, page 16
        // configurationVersion
        sequenceHeader.writeByte(0x01);
        // AVCProfileIndication
        sequenceHeader.writeByte(profile_idc);
        // profile_compatibility
        sequenceHeader.writeByte(0x00);
        // AVCLevelIndication
        sequenceHeader.writeByte(level_idc);
        // lengthSizeMinusOne, or NAL_unit_length, always use 4bytes size,
        // so we always set it to 0x03.
        sequenceHeader.writeByte(0x03);

        // sps
        // 5.3.4.2.1 Syntax, H.264-AVC-ISO_IEC_14496-15.pdf, page 16
        // numOfSequenceParameterSets, always 1
        sequenceHeader.writeByte(0x01);
        // sequenceParameterSetLength
        sequenceHeader.writeShort(sps.readableBytes());
        // sequenceParameterSetNALUnit
        sequenceHeader.writeBytes(sps);


        // pps
        // 5.3.4.2.1 Syntax, H.264-AVC-ISO_IEC_14496-15.pdf, page 16
        // numOfPictureParameterSets, always 1
        sequenceHeader.writeByte(0x01);
        // pictureParameterSetLength
        sequenceHeader.writeShort(pps.readableBytes());

        // pictureParameterSetNALUnit
        sequenceHeader.writeBytes(pps);
    }
}
