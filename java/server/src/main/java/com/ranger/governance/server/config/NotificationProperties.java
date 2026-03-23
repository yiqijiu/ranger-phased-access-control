package com.ranger.governance.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "governance.notify")
public class NotificationProperties {
    private String channel = "noop";
    private int connectTimeoutMs = 200;
    private int readTimeoutMs = 1000;
    private Channel feishu = new Channel();
    private Channel slack = new Channel();
    private Channel dingtalk = new Channel();
    private Channel wecom = new Channel();
    private Channel teams = new Channel();

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public Channel getFeishu() {
        return feishu;
    }

    public void setFeishu(Channel feishu) {
        this.feishu = feishu;
    }

    public Channel getSlack() {
        return slack;
    }

    public void setSlack(Channel slack) {
        this.slack = slack;
    }

    public Channel getDingtalk() {
        return dingtalk;
    }

    public void setDingtalk(Channel dingtalk) {
        this.dingtalk = dingtalk;
    }

    public Channel getWecom() {
        return wecom;
    }

    public void setWecom(Channel wecom) {
        this.wecom = wecom;
    }

    public Channel getTeams() {
        return teams;
    }

    public void setTeams(Channel teams) {
        this.teams = teams;
    }

    public static class Channel {
        private String webhookUrl = "";

        public String getWebhookUrl() {
            return webhookUrl;
        }

        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }
    }
}
