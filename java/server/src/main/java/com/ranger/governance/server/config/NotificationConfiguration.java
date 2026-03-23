package com.ranger.governance.server.config;

import com.ranger.governance.server.service.MsgNotifier;
import com.ranger.governance.server.service.NoopMsgNotifier;
import com.ranger.governance.server.service.notify.ChannelMsgNotifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Configuration
public class NotificationConfiguration {

    @Bean
    public NoopMsgNotifier noopMsgNotifier() {
        return new NoopMsgNotifier();
    }

    @Bean
    @Primary
    public MsgNotifier msgNotifier(
            NotificationProperties properties,
            List<ChannelMsgNotifier> channelNotifiers,
            NoopMsgNotifier noopMsgNotifier
    ) {
        String channel = normalizeChannel(properties.getChannel());
        if ("noop".equals(channel)) {
            return noopMsgNotifier;
        }

        Map<String, ChannelMsgNotifier> registry = new LinkedHashMap<String, ChannelMsgNotifier>();
        for (ChannelMsgNotifier notifier : channelNotifiers) {
            registry.put(normalizeChannel(notifier.channel()), notifier);
        }

        ChannelMsgNotifier notifier = registry.get(channel);
        if (notifier == null) {
            throw new IllegalStateException("Unsupported governance.notify.channel=" + properties.getChannel());
        }

        validateChannelConfig(channel, properties);
        return notifier;
    }

    private void validateChannelConfig(String channel, NotificationProperties properties) {
        String webhookUrl;
        if ("feishu".equals(channel)) {
            webhookUrl = properties.getFeishu().getWebhookUrl();
        } else if ("slack".equals(channel)) {
            webhookUrl = properties.getSlack().getWebhookUrl();
        } else if ("dingtalk".equals(channel)) {
            webhookUrl = properties.getDingtalk().getWebhookUrl();
        } else if ("wecom".equals(channel)) {
            webhookUrl = properties.getWecom().getWebhookUrl();
        } else if ("teams".equals(channel)) {
            webhookUrl = properties.getTeams().getWebhookUrl();
        } else {
            throw new IllegalStateException("Unsupported governance.notify.channel=" + channel);
        }

        if (!hasText(webhookUrl)) {
            throw new IllegalStateException(
                    "Missing webhook for governance.notify.channel=" + channel
                            + ", please set governance.notify." + channel + ".webhook-url"
            );
        }
    }

    private String normalizeChannel(String value) {
        return value == null ? "noop" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
