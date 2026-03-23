package com.ranger.governance.server.service.notify;

import com.ranger.governance.server.config.NotificationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class FeishuMsgNotifier implements ChannelMsgNotifier {
    private final NotificationProperties properties;
    private final WebhookSender sender;

    public FeishuMsgNotifier(NotificationProperties properties, WebhookSender sender) {
        this.properties = properties;
        this.sender = sender;
    }

    @Override
    public String channel() {
        return "feishu";
    }

    @Override
    public void send(String receiver, String title, String content) {
        Map<String, Object> text = new LinkedHashMap<String, Object>();
        text.put("text", buildMessage(receiver, title, content));

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("msg_type", "text");
        payload.put("content", text);

        sender.postJson(properties.getFeishu().getWebhookUrl(), payload);
    }

    private String buildMessage(String receiver, String title, String content) {
        return "[Governance][" + safe(title) + "]\nreceiver: " + safe(receiver) + "\n" + safe(content);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
