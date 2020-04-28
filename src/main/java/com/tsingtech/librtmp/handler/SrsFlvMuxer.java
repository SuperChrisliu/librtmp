package com.tsingtech.librtmp.handler;

import com.github.faucamp.simplertmp.packets.Video;
import io.netty.channel.ChannelHandlerContext;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class SrsFlvMuxer {
    private static final int VIDEO_ALLOC_SIZE = 128 * 1024;

    private SrsFlv flv = new SrsFlv();
    private SrsAllocator mVideoAllocator = new SrsAllocator(VIDEO_ALLOC_SIZE);
    ChannelHandlerContext ctx;
    int currentStreamId;
    public SrsFlvMuxer(ChannelHandlerContext ctx, int currentStreamId) {
        this.ctx = ctx;
        this.currentStreamId = currentStreamId;
    }



    public void writeSampleData(ByteBuffer byteBuf, int type) {
        flv.writeVideoSample(byteBuf, type);
    }

    public void setPPSSPS(ByteBuffer h264_pps, ByteBuffer h264_sps) {
        flv.setPPSSPS(h264_pps, h264_sps);
    }

    // E.4.3.1 VIDEODATA
    // Frame Type UB [4]
    // Type of video frame. The following values are defined:
    //     1 = key frame (for AVC, a seekable frame)
    //     2 = inter frame (for AVC, a non-seekable frame)
    //     3 = disposable inter frame (H.263 only)
    //     4 = generated key frame (reserved for server use only)
    //     5 = video info/command frame
    public class SrsCodecVideoAVCFrame
    {
        // set to the zero to reserved, for array map.
        public final static int Reserved = 0;
        public final static int Reserved1 = 6;

        public final static int KeyFrame                     = 1;
        public final static int InterFrame                 = 2;
        public final static int DisposableInterFrame         = 3;
        public final static int GeneratedKeyFrame            = 4;
        public final static int VideoInfoFrame                = 5;
    }

    // AVCPacketType IF CodecID == 7 UI8
    // The following values are defined:
    //     0 = AVC sequence header
    //     1 = AVC NALU
    //     2 = AVC end of sequence (lower level NALU sequence ender is
    //         not required or supported)
    public class SrsCodecVideoAVCType
    {
        // set to the max value to reserved, for array map.
        public final static int Reserved                    = 3;

        public final static int SequenceHeader                 = 0;
        public final static int NALU                         = 1;
        public final static int SequenceHeaderEOF             = 2;
    }

    /**
     * E.4.1 FLV Tag, page 75
     */
    public class SrsCodecFlvTag
    {
        // set to the zero to reserved, for array map.
        public final static int Reserved = 0;

        // 8 = audio
        public final static int Audio = 8;
        // 9 = video
        public final static int Video = 9;
        // 18 = script data
        public final static int Script = 18;
    };

    // E.4.3.1 VIDEODATA
    // CodecID UB [4]
    // Codec Identifier. The following values are defined:
    //     2 = Sorenson H.263
    //     3 = Screen video
    //     4 = On2 VP6
    //     5 = On2 VP6 with alpha channel
    //     6 = Screen video version 2
    //     7 = AVC
    public class SrsCodecVideo
    {
        // set to the zero to reserved, for array map.
        public final static int Reserved                = 0;
        public final static int Reserved1                = 1;
        public final static int Reserved2                = 9;

        // for user to disable video, for example, use pure audio hls.
        public final static int Disabled                = 8;

        public final static int SorensonH263             = 2;
        public final static int ScreenVideo             = 3;
        public final static int On2VP6                 = 4;
        public final static int On2VP6WithAlphaChannel = 5;
        public final static int ScreenVideoVersion2     = 6;
        public final static int AVC                     = 7;
    }

    /**
     * Table 7-1 – NAL unit type codes, syntax element categories, and NAL unit type classes
     * H.264-AVC-ISO_IEC_14496-10-2012.pdf, page 83.
     */
    public class SrsAvcNaluType
    {
        // Unspecified
        public final static int Reserved = 0;

        // Coded slice of a non-IDR picture slice_layer_without_partitioning_rbsp( )
        public final static int NonIDR = 1;
        // Coded slice data partition A slice_data_partition_a_layer_rbsp( )
        public final static int DataPartitionA = 2;
        // Coded slice data partition B slice_data_partition_b_layer_rbsp( )
        public final static int DataPartitionB = 3;
        // Coded slice data partition C slice_data_partition_c_layer_rbsp( )
        public final static int DataPartitionC = 4;
        // Coded slice of an IDR picture slice_layer_without_partitioning_rbsp( )
        public final static int IDR = 5;
        // Supplemental enhancement information (SEI) sei_rbsp( )
        public final static int SEI = 6;
        // Sequence parameter set seq_parameter_set_rbsp( )
        public final static int SPS = 7;
        // Picture parameter set pic_parameter_set_rbsp( )
        public final static int PPS = 8;
        // Access unit delimiter access_unit_delimiter_rbsp( )
        public final static int AccessUnitDelimiter = 9;
        // End of sequence end_of_seq_rbsp( )
        public final static int EOSequence = 10;
        // End of stream end_of_stream_rbsp( )
        public final static int EOStream = 11;
        // Filler data filler_data_rbsp( )
        public final static int FilterData = 12;
        // Sequence parameter set extension seq_parameter_set_extension_rbsp( )
        public final static int SPSExt = 13;
        // Prefix NAL unit prefix_nal_unit_rbsp( )
        public final static int PrefixNALU = 14;
        // Subset sequence parameter set subset_seq_parameter_set_rbsp( )
        public final static int SubsetSPS = 15;
        // Coded slice of an auxiliary coded picture without partitioning slice_layer_without_partitioning_rbsp( )
        public final static int LayerWithoutPartition = 19;
        // Coded slice extension slice_layer_extension_rbsp( )
        public final static int CodedSliceExt = 20;
    }

    /**
     * the search result for annexb.
     */
    private class SrsAnnexbSearch {
        public int nb_start_code = 0;
        public boolean match = false;
    }

    /**
     * the demuxed tag frame.
     */
    private class SrsFlvFrameBytes {
        public ByteBuffer data;
        public int size;
    }

    /**
     * the muxed flv frame.
     */
    private class SrsFlvFrame {
        // the tag bytes.
        public SrsAllocator.Allocation flvTag;
        // the codec type for audio/aac and video/avc for instance.
        public int avc_aac_type;
        // the frame type, keyframe or not.
        public int frame_type;
        // the tag type, audio, video or data.
        public int type;
        // the dts in ms, tbn is 1000.
        public int dts;

        public boolean isKeyFrame() {
            return isVideo() && frame_type == SrsCodecVideoAVCFrame.KeyFrame;
        }

        public boolean isSequenceHeader() {
            return avc_aac_type == 0;
        }

        public boolean isVideo() {
            return type == SrsCodecFlvTag.Video;
        }

        public boolean isAudio() {
            return type == SrsCodecFlvTag.Audio;
        }
    }

    /**
     * the raw h.264 stream, in annexb.
     */
    private class SrsRawH264Stream {
        private final static String TAG = "SrsFlvMuxer";

        private SrsAnnexbSearch annexb = new SrsAnnexbSearch();
        private SrsFlvFrameBytes seq_hdr = new SrsFlvFrameBytes();
        private SrsFlvFrameBytes sps_hdr = new SrsFlvFrameBytes();
        private SrsFlvFrameBytes sps_bb = new SrsFlvFrameBytes();
        private SrsFlvFrameBytes pps_hdr = new SrsFlvFrameBytes();
        private SrsFlvFrameBytes pps_bb = new SrsFlvFrameBytes();

        public boolean isSps(SrsFlvFrameBytes frame) {
            return frame.size >= 1 && (frame.data.get(0) & 0x1f) == SrsAvcNaluType.SPS;
        }

        public boolean isPps(SrsFlvFrameBytes frame) {
            return frame.size >= 1 && (frame.data.get(0) & 0x1f) == SrsAvcNaluType.PPS;
        }

        public SrsFlvFrameBytes muxNaluHeader(SrsFlvFrameBytes frame) {
            SrsFlvFrameBytes nalu_hdr = new SrsFlvFrameBytes();
            nalu_hdr.data = ByteBuffer.allocate(4);
            nalu_hdr.size = 4;
            // 5.3.4.2.1 Syntax, H.264-AVC-ISO_IEC_14496-15.pdf, page 16
            // lengthSizeMinusOne, or NAL_unit_length, always use 4bytes size
            int NAL_unit_length = frame.size;

            // mux the avc NALU in "ISO Base Media File Format"
            // from H.264-AVC-ISO_IEC_14496-15.pdf, page 20
            // NALUnitLength
            nalu_hdr.data.putInt(NAL_unit_length);

            // reset the buffer.
            nalu_hdr.data.rewind();
            return nalu_hdr;
        }

        public void muxSequenceHeader(ByteBuffer sps, ByteBuffer pps, int dts, int pts,
                                        ArrayList<SrsFlvFrameBytes> frames) {
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
            if (seq_hdr.data == null) {
                seq_hdr.data = ByteBuffer.allocate(5);
                seq_hdr.size = 5;
            }
            seq_hdr.data.rewind();
            // @see: Annex A Profiles and levels, H.264-AVC-ISO_IEC_14496-10.pdf, page 205
            //      Baseline profile profile_idc is 66(0x42).
            //      Main profile profile_idc is 77(0x4d).
            //      Extended profile profile_idc is 88(0x58).
            byte profile_idc = sps.get(1);
            //u_int8_t constraint_set = frame[2];
            byte level_idc = sps.get(3);

            // generate the sps/pps header
            // 5.3.4.2.1 Syntax, H.264-AVC-ISO_IEC_14496-15.pdf, page 16
            // configurationVersion
            seq_hdr.data.put((byte) 0x01);
            // AVCProfileIndication
            seq_hdr.data.put(profile_idc);
            // profile_compatibility
            seq_hdr.data.put((byte) 0x00);
            // AVCLevelIndication
            seq_hdr.data.put(level_idc);
            // lengthSizeMinusOne, or NAL_unit_length, always use 4bytes size,
            // so we always set it to 0x03.
            seq_hdr.data.put((byte) 0x03);

            // reset the buffer.
            seq_hdr.data.rewind();
            frames.add(seq_hdr);

            // sps
            if (sps_hdr.data == null) {
                sps_hdr.data = ByteBuffer.allocate(3);
                sps_hdr.size = 3;
            }
            sps_hdr.data.rewind();
            // 5.3.4.2.1 Syntax, H.264-AVC-ISO_IEC_14496-15.pdf, page 16
            // numOfSequenceParameterSets, always 1
            sps_hdr.data.put((byte) 0x01);
            // sequenceParameterSetLength
            sps_hdr.data.putShort((short) sps.array().length);

            sps_hdr.data.rewind();
            frames.add(sps_hdr);

            // sequenceParameterSetNALUnit
            sps_bb.size = sps.array().length;
            sps_bb.data = sps.duplicate();
            frames.add(sps_bb);

            // pps
            if (pps_hdr.data == null) {
                pps_hdr.data = ByteBuffer.allocate(3);
                pps_hdr.size = 3;
            }
            pps_hdr.data.rewind();
            // 5.3.4.2.1 Syntax, H.264-AVC-ISO_IEC_14496-15.pdf, page 16
            // numOfPictureParameterSets, always 1
            pps_hdr.data.put((byte) 0x01);
            // pictureParameterSetLength
            pps_hdr.data.putShort((short) pps.array().length);

            pps_hdr.data.rewind();
            frames.add(pps_hdr);

            // pictureParameterSetNALUnit
            pps_bb.size = pps.array().length;
            pps_bb.data = pps.duplicate();
            frames.add(pps_bb);
        }

        public SrsAllocator.Allocation muxFlvTag(ArrayList<SrsFlvFrameBytes> frames, int frame_type,
                                                 int avc_packet_type, int dts, int pts) {
            // for h264 in RTMP video payload, there is 5bytes header:
            //      1bytes, FrameType | CodecID
            //      1bytes, AVCPacketType
            //      3bytes, CompositionTime, the cts.
            // @see: E.4.3 Video Tags, video_file_format_spec_v10_1.pdf, page 78
            int size = 5;
            for (int i = 0; i < frames.size(); i++) {
                size += frames.get(i).size;
            }
            SrsAllocator.Allocation allocation = mVideoAllocator.allocate(size);

            // @see: E.4.3 Video Tags, video_file_format_spec_v10_1.pdf, page 78
            // Frame Type, Type of video frame.
            // CodecID, Codec Identifier.
            // set the rtmp header
            allocation.put((byte) ((frame_type << 4) | SrsCodecVideo.AVC));

            // AVCPacketType
            allocation.put((byte)avc_packet_type);

            // CompositionTime
            // pts = dts + cts, or
            // cts = pts - dts.
            // where cts is the header in rtmp video packet payload header.
            int cts = pts - dts;
            allocation.put((byte)(cts >> 16));
            allocation.put((byte)(cts >> 8));
            allocation.put((byte)cts);

            // h.264 raw data.
            for (int i = 0; i < frames.size(); i++) {
                SrsFlvFrameBytes frame = frames.get(i);
                frame.data.get(allocation.array(), allocation.size(), frame.size);
                allocation.appendOffset(frame.size);
            }

            return allocation;
        }

    }

    /**
     * remux the annexb to flv tags.
     */
    private class SrsFlv {
        private int achannel;
        private int asample_rate;
        private SrsRawH264Stream avc = new SrsRawH264Stream();
        private ArrayList<SrsFlvFrameBytes> ipbs = new ArrayList<>();
        private SrsAllocator.Allocation audio_tag;
        private SrsAllocator.Allocation video_tag;
        private ByteBuffer h264_sps;
        private boolean h264_sps_changed;
        private ByteBuffer h264_pps;
        private boolean h264_pps_changed;
        private boolean h264_sps_pps_sent;
        private boolean aac_specific_config_got;

        public SrsFlv() {
            reset();
        }

        public void reset() {
            h264_sps_changed = false;
            h264_pps_changed = false;
            h264_sps_pps_sent = false;
            aac_specific_config_got = false;
            if (null != h264_sps){
                Arrays.fill(h264_sps.array(),(byte) 0x00);
                h264_sps.clear();
            }
            if (null!=h264_pps) {
                Arrays.fill(h264_pps.array(),(byte) 0x00);
                h264_pps.clear();
            }

        }

        void setPPSSPS (ByteBuffer h264_pps, ByteBuffer h264_sps) {
            d += 1000 / 25;
            p = d;
            h264_pps_changed = true;
            this.h264_pps = h264_sps;
            this.h264_sps = h264_pps;
            writeH264SpsPps(d, p);
        }
        int d = 0;
        int p = 0;

        public void writeVideoSample(final ByteBuffer bb, int type) {
            d += 1000 / 25;
            p = d;
            SrsFlvFrameBytes frame = new SrsFlvFrameBytes();
            frame.data = bb;
            frame.size = bb.capacity();
//            int nal_unit_type = frame.data.get(0) & 0x1f;
//            if (nal_unit_type == SrsAvcNaluType.IDR) {
//                type = SrsCodecVideoAVCFrame.KeyFrame;
//            }

            ipbs.add(avc.muxNaluHeader(frame));
            ipbs.add(frame);

            //writeH264SpsPps(dts, pts);
            writeH264IpbFrame(ipbs, type, d, p);
            ipbs.clear();
        }

        private void writeH264SpsPps(int dts, int pts) {
            // when sps or pps changed, update the sequence header,
            // for the pps maybe not changed while sps changed.
            // so, we must check when each video ts message frame parsed.
            if (h264_sps_pps_sent && !h264_sps_changed && !h264_pps_changed) {
                return;
            }

            // when not got sps/pps, wait.
            if (h264_pps == null || h264_sps == null) {
                return;
            }

            // h264 raw to h264 packet.
            ArrayList<SrsFlvFrameBytes> frames = new ArrayList<>();
            avc.muxSequenceHeader(h264_sps, h264_pps, dts, pts, frames);

            // h264 packet to flv packet.
            int frame_type = SrsCodecVideoAVCFrame.KeyFrame;
            int avc_packet_type = SrsCodecVideoAVCType.SequenceHeader;
            video_tag = avc.muxFlvTag(frames, frame_type, avc_packet_type, dts, pts);

            // the timestamp in rtmp message header is dts.
            writeRtmpPacket(SrsCodecFlvTag.Video, dts, frame_type, avc_packet_type, video_tag);

            // reset sps and pps.
            h264_sps_changed = false;
            h264_pps_changed = false;
            h264_sps_pps_sent = true;
            System.out.println(String.format("flv: h264 sps/pps sent, sps=%dB, pps=%dB",
                h264_sps.array().length, h264_pps.array().length));
        }

        private void writeH264IpbFrame(ArrayList<SrsFlvFrameBytes> frames, int type, int dts, int pts) {
            // when sps or pps not sent, ignore the packet.
            // @see https://github.com/simple-rtmp-server/srs/issues/203
            if (!h264_sps_pps_sent) {
                return;
            }

            video_tag = avc.muxFlvTag(frames, type, SrsCodecVideoAVCType.NALU, dts, pts);

            // the timestamp in rtmp message header is dts.
            writeRtmpPacket(SrsCodecFlvTag.Video, dts, type, SrsCodecVideoAVCType.NALU, video_tag);
        }

        private void writeRtmpPacket(int type, int dts, int frame_type, int avc_aac_type, SrsAllocator.Allocation tag) {
            SrsFlvFrame frame = new SrsFlvFrame();
            frame.flvTag = tag;
            frame.type = type;
            frame.dts = dts;
            frame.frame_type = frame_type;
            frame.avc_aac_type = avc_aac_type;
            publishVideoData(frame.flvTag.array(), frame.flvTag.size(), frame.dts);
        }

        public void publishVideoData(byte[] data, int size, int dts) {
            Video video = new Video();
            video.setData(data, size);
            video.getHeader().setAbsoluteTimestamp(dts);
            video.getHeader().setMessageStreamId(currentStreamId);
            ctx.writeAndFlush(video);
        }
    }
}
