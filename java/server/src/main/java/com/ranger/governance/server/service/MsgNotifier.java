package com.ranger.governance.server.service;

public interface MsgNotifier {
    void send(String receiver, String title, String content);
}
