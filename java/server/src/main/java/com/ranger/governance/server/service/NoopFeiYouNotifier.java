package com.ranger.governance.server.service;

public class NoopFeiYouNotifier implements FeiYouNotifier {
    @Override
    public void send(String receiver, String title, String content) {
        // no-op placeholder for integration with FeiYou webhook or MQ consumer.
    }
}
