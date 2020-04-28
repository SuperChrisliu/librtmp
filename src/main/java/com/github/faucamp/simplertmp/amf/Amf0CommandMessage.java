package com.github.faucamp.simplertmp.amf;

/**
 * Author: chrisliu
 * Date: 2019/8/19 13:47
 * Mail: gwarmdll@gmail.com
 */
public enum Amf0CommandMessage {
    AMF0_COMMAND_ON_BW_DONE("onBWDone"), AMF0_COMMAND_ON_STATUS("onStatus"), AMF0_COMMAND_RESULT("_result"), AMF0_COMMAND_ERROR("_error");

    private String message;

    Amf0CommandMessage (String message) {
        this.message = message;
    }
}
