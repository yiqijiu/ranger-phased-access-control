package com.ranger.governance.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoopMsgNotifier implements MsgNotifier {
    private static final Logger LOG = LoggerFactory.getLogger(NoopMsgNotifier.class);

    @Override
    public void send(String receiver, String title, String content) {
        LOG.info("notify skipped, receiver={}, title={}, content={}", receiver, title, content);
    }
}
