package com.ranger.governance.server.service.notify;

import com.ranger.governance.server.service.MsgNotifier;

public interface ChannelMsgNotifier extends MsgNotifier {
    String channel();
}
