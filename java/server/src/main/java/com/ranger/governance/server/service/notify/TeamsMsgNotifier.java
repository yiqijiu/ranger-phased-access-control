package com.ranger.governance.server.service.notify;

import com.ranger.governance.server.config.NotificationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TeamsMsgNotifier implements ChannelMsgNotifier {
    private final NotificationProperties properties;
    private final WebhookSender sender;

    public TeamsMsgNotifier(NotificationProperties properties, WebhookSender sender) {
        this.properties = properties;
        this.sender = sender;
    }

    @Override
    public String channel() {
        return "teams";
    }

    @Override
    public void send(String receiver, String title, String content) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("text", "[Governance][" + safe(title) + "] receiver=" + safe(receiver) + " content=" + safe(content));
        sender.postJson(properties.getTeams().getWebhookUrl(), payload);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
