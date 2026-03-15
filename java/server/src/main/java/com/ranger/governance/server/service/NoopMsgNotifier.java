package com.ranger.governance.server.service;

public class NoopMsgNotifier implements MsgNotifier {
    @Override
    public void send(String receiver, String title, String content) {
    }
}
