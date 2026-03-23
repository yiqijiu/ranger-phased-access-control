package com.ranger.governance.server.service.notify;

import com.ranger.governance.server.config.NotificationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WeComMsgNotifier implements ChannelMsgNotifier {
    private final NotificationProperties properties;
    private final WebhookSender sender;

    public WeComMsgNotifier(NotificationProperties properties, WebhookSender sender) {
        this.properties = properties;
        this.sender = sender;
    }

    @Override
    public String channel() {
        return "wecom";
    }

    @Override
    public void send(String receiver, String title, String content) {
        Map<String, Object> text = new LinkedHashMap<String, Object>();
        text.put("content", "[Governance][" + safe(title) + "]\nreceiver: " + safe(receiver) + "\n" + safe(content));

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("msgtype", "text");
        payload.put("text", text);

        sender.postJson(properties.getWecom().getWebhookUrl(), payload);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
