package com.ranger.governance.server.config;

import com.ranger.governance.server.service.MsgNotifier;
import com.ranger.governance.server.service.NoopMsgNotifier;
import com.ranger.governance.server.service.notify.ChannelMsgNotifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

class NotificationConfigurationTest {

    private final NotificationConfiguration configuration = new NotificationConfiguration();

    @Test
    void shouldUseNoopNotifierByDefault() {
        NotificationProperties properties = new NotificationProperties();

        MsgNotifier notifier = configuration.msgNotifier(
                properties,
                Collections.<ChannelMsgNotifier>singletonList(new StubChannelNotifier("feishu")),
                new NoopMsgNotifier()
        );

        Assertions.assertTrue(notifier instanceof NoopMsgNotifier);
    }

    @Test
    void shouldSelectConfiguredChannelNotifier() {
        NotificationProperties properties = new NotificationProperties();
        properties.setChannel("slack");
        properties.getSlack().setWebhookUrl("https://example.com/slack");
        StubChannelNotifier slack = new StubChannelNotifier("slack");

        MsgNotifier notifier = configuration.msgNotifier(
                properties,
                Arrays.<ChannelMsgNotifier>asList(new StubChannelNotifier("feishu"), slack),
                new NoopMsgNotifier()
        );

        Assertions.assertSame(slack, notifier);
    }

    @Test
    void shouldRejectUnsupportedChannel() {
        NotificationProperties properties = new NotificationProperties();
        properties.setChannel("telegram");

        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () -> configuration.msgNotifier(
                        properties,
                        Collections.<ChannelMsgNotifier>singletonList(new StubChannelNotifier("feishu")),
                        new NoopMsgNotifier()
                )
        );

        Assertions.assertTrue(ex.getMessage().contains("Unsupported governance.notify.channel"));
    }

    @Test
    void shouldRejectMissingWebhookForSelectedChannel() {
        NotificationProperties properties = new NotificationProperties();
        properties.setChannel("teams");

        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () -> configuration.msgNotifier(
                        properties,
                        Collections.<ChannelMsgNotifier>singletonList(new StubChannelNotifier("teams")),
                        new NoopMsgNotifier()
                )
        );

        Assertions.assertTrue(ex.getMessage().contains("Missing webhook"));
    }

    private static class StubChannelNotifier implements ChannelMsgNotifier {
        private final String channel;

        private StubChannelNotifier(String channel) {
            this.channel = channel;
        }

        @Override
        public String channel() {
            return channel;
        }

        @Override
        public void send(String receiver, String title, String content) {
        }
    }
}
