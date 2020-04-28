package com.github.faucamp.simplertmp.amf;

/**
 * Author: chrisliu
 * Date: 2019/8/19 13:42
 * Mail: gwarmdll@gmail.com
 */
public enum Amf0CommandName {
    AMF0_COMMAND_CONNECT("connect"), AMF0_COMMAND_CREATE_STREAM("createStream"), AMF0_COMMAND_CLOSE_STREAM("closeStream"),
    AMF0_COMMAND_PLAY("play"), AMF0_COMMAND_PAUSE("pause"), AMF0_COMMAND_RELEASE_STREAM("releaseStream"),
    AMF0_COMMAND_FC_PUBLISH("FCPublish"), AMF0_COMMAND_UNPUBLISH("FCUnpublish"), AMF0_COMMAND_PUBLISH("publish");

    private String commandName;

    Amf0CommandName(String name) {
        this.commandName = name;
    }

    public String getCommandName() {
        return this.commandName;
    }
}
