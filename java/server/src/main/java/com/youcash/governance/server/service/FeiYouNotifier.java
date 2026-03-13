package com.youcash.governance.server.service;

public interface FeiYouNotifier {
    void send(String receiver, String title, String content);
}
