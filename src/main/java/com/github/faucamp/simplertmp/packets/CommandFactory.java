package com.github.faucamp.simplertmp.packets;

import com.github.faucamp.simplertmp.amf.Amf0CommandName;
import com.github.faucamp.simplertmp.amf.AmfMap;
import com.github.faucamp.simplertmp.amf.AmfNull;
import com.github.faucamp.simplertmp.amf.AmfObject;
import com.github.faucamp.simplertmp.io.ChunkStreamInfo;

/**
 * Author: chrisliu
 * Date: 2019/8/19 14:01
 * Mail: gwarmdll@gmail.com
 */
public class CommandFactory {

    public static Command connectCommand () {
        Command invoke = new Command(Amf0CommandName.AMF0_COMMAND_CONNECT.getCommandName(), 1);
        invoke.getHeader().setMessageStreamId(0);
        AmfObject args = new AmfObject();
        args.setProperty("app", "live");
        args.setProperty("flashVer", "WIN 15,0,0,239");
//        args.setProperty("swfUrl", swfUrl);
        args.setProperty("tcUrl", "rtmp://106.15.73.146/qywslive");
        args.setProperty("fpad", false);
        args.setProperty("capabilities", 239);
        args.setProperty("audioCodecs", 3575);
        args.setProperty("videoCodecs", 252);
        args.setProperty("videoFunction", 1);
//        args.setProperty("pageUrl", pageUrl);
        args.setProperty("objectEncoding", 0);
        invoke.addData(args);
        return invoke;
    }

    public static Command createStream () {
        Command createStream = new Command(Amf0CommandName.AMF0_COMMAND_CREATE_STREAM.getCommandName(), 2);
        createStream.getHeader().setMessageStreamId(0);
        createStream.addData(new AmfNull());
        return createStream;
    }

    public static Command publish (int sid, String streamName) {
        Command publish = new Command(Amf0CommandName.AMF0_COMMAND_PUBLISH.getCommandName(), 3, ChunkStreamInfo.RTMP_CID_OVER_STREAM);
        publish.getHeader().setMessageStreamId(sid);
        publish.addData(new AmfNull());
        publish.addData(streamName);
        publish.addData("live");
        return publish;
    }

    public static Command setChunkSize () {
        Command createStream = new Command(Amf0CommandName.AMF0_COMMAND_CREATE_STREAM.getCommandName(), 2);
        createStream.getHeader().setMessageStreamId(0);
        createStream.addData(new AmfNull());
        return createStream;
    }
}
